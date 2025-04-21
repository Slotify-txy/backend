package org.slotify.notificationservice.service.impl;

import com.google.protobuf.util.JsonFormat;
import io.awspring.cloud.sqs.annotation.SnsNotificationMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.util.RandomUidGenerator;
import net.fortuna.ical4j.util.UidGenerator;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slotify.notificationservice.grpc.EmailTokenServiceGrpcClient;
import org.slotify.notificationservice.util.TimestampConvertor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import slot.Slot;
import user.Coach;
import user.Student;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class EmailService {
    @Value("${spring.cloud.aws.ses.from}")
    private String senderEmail;

    @Value("${frontend_url.student}")
    private String studentFrontendUrl;

    @Value("${frontend_url.coach}")
    private String coachFrontendUrl;

    @Value("${backend_url}")
    private String backendUrl;

    private String backendApiPrefix;
    private final TemplateEngine templateEngine;
    private final DateTimeFormatter startFormatterWithMin = DateTimeFormatter.ofPattern("E MMM d, yyyy h:mm a", Locale.ENGLISH);
    private final DateTimeFormatter startFormatter = DateTimeFormatter.ofPattern("E MMM d, yyyy h a", Locale.ENGLISH);
    private final DateTimeFormatter endFormatterWithMin = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH); // start time and end time must be in the same day
    private final DateTimeFormatter endFormatter = DateTimeFormatter.ofPattern("h a", Locale.ENGLISH); // start time and end time must be in the same day
    private final EmailTokenServiceGrpcClient emailTokenServiceGrpcClient;
    private final JavaMailSender mailSender = new JavaMailSenderImpl();
    private static final Logger log = LoggerFactory.getLogger(
            EmailService.class);

    @PostConstruct
    public void init() {
        this.backendApiPrefix = backendUrl + "api/v1/slot";
    }

    @SqsListener("${spring.cloud.aws.sqs.queue.open-hour-update}")
    public void listenToOpenHourUpdate(@SnsNotificationMessage final String message) {
        try {
            Coach.Builder builder = Coach.newBuilder();
            JsonFormat.parser().merge(message, builder);
            Coach coach = builder.build();
            sendOpenHourUpdateEmail(coach);
            log.info("Open hour update emails sent for Coach, id: {}", coach.getId());
        } catch (Exception e) {
            log.error("Couldn't send open hour update emails: {}", e.getMessage());
        }
    }

    @SqsListener("${spring.cloud.aws.sqs.queue.slot-status-update}")
    public void listenToSlotStatusUpdate(@SnsNotificationMessage final String message) {
        try {
            Slot.Builder builder = Slot.newBuilder();
            JsonFormat.parser().merge(message, builder);
            Slot slot = builder.build();
            sendSlotStatusUpdateEmail(slot);
            log.info("Slot status update emails sent, id: {}, student_id: {}, coach_id: {}, status: {}", slot.getId(), slot.getStudent().getId(), slot.getCoach().getId(), slot.getStatus());
        } catch (Exception e) {
            log.error("Couldn't send slot status update  emails: {}", e.getMessage());
        }
    }

    @Async
    public void sendOpenHourUpdateEmail(Coach coach) throws MessagingException, IOException {
        List<Student> students = coach.getStudentsList();
        for (Student student : students) {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            String studentName = WordUtils.capitalizeFully(student.getName().split(" ")[0].toLowerCase());
            String coachName = WordUtils.capitalizeFully(coach.getName().split(" ")[0].toLowerCase());
            helper.setFrom(new InternetAddress(senderEmail, "Slotify"));
            helper.setTo(student.getEmail());
            helper.setSubject(coachName + " has updated their open hours!");

            Map<String, Object> emailVariables = new HashMap<>();
            emailVariables.put("studentName", studentName);
            emailVariables.put("coachName", coachName);
            emailVariables.put("websiteUrl", studentFrontendUrl);

            Context context = new Context();
            context.setVariables(emailVariables);
            String emailContent = templateEngine.process("open-hour-update", context);

            helper.setText(emailContent, true);

            mailSender.send(message);
            log.info("Open hour update email sent to student: {}", studentName);
        }

    }

    @Async
    public void sendSlotStatusUpdateEmail(Slot slot) throws MessagingException, IOException {
        switch (slot.getStatus()) {
            case PENDING -> sendConfirmationEmail(slot);
            case APPOINTMENT -> sendAppointmentEmail(slot);
            case REJECTED -> sendRejectedEmail(slot);
            case CANCELLED -> sendCancelledEmail(slot);
            default -> log.error("unexpected status");
        }
    }

    private void sendConfirmationEmail(Slot slot) throws MessagingException, IOException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        Student student = slot.getStudent();
        Coach coach = slot.getCoach();

        String token = emailTokenServiceGrpcClient.generateToken(slot.getId(), slot.getStartAt());
        String studentName = WordUtils.capitalizeFully(student.getName().split(" ")[0].toLowerCase());
        String coachName = WordUtils.capitalizeFully(coach.getName().split(" ")[0].toLowerCase());

        LocalDateTime startAt = TimestampConvertor.convertFromProtoTimestampToLocalDateTime(slot.getStartAt());
        LocalDateTime endAt = TimestampConvertor.convertFromProtoTimestampToLocalDateTime(slot.getEndAt());

        String time = startAt.format(startAt.getMinute() == 0 ? startFormatter: startFormatterWithMin) + " - " + endAt.format(endAt.getMinute() == 0? endFormatter : endFormatterWithMin);

        helper.setFrom(new InternetAddress(senderEmail, "Slotify"));
        helper.setTo(student.getEmail());
        helper.setSubject("Invitation from " + coachName + " @ " + time);

        Map<String, Object> emailVariables = new HashMap<>();
        emailVariables.put("studentName", studentName);
        emailVariables.put("coachName", coachName);
        emailVariables.put("time", time);
        emailVariables.put("websiteUrl", studentFrontendUrl);
        emailVariables.put("acceptUrl", backendApiPrefix + "/" + slot.getId() + "/token/" + token + "?status=APPOINTMENT");
        emailVariables.put("rejectUrl", backendApiPrefix + "/" + slot.getId() + "/token/" + token + "?status=REJECTED");

        Context context = new Context();
        context.setVariables(emailVariables);
        String emailContent = templateEngine.process("confirmation", context);

        helper.setText(emailContent, true);

        mailSender.send(message);
        log.info("Confirmation email sent to student: {}", studentName);
    }

    private void sendAppointmentEmail(Slot slot) throws MessagingException, IOException {
        String token = emailTokenServiceGrpcClient.generateToken(slot.getId(), slot.getStartAt());
        Student student = slot.getStudent();
        Coach coach = slot.getCoach();
        String studentName = WordUtils.capitalizeFully(student.getName().split(" ")[0].toLowerCase());
        String coachName = WordUtils.capitalizeFully(coach.getName().split(" ")[0].toLowerCase());

        LocalDateTime startAt = TimestampConvertor.convertFromProtoTimestampToLocalDateTime(slot.getStartAt());
        LocalDateTime endAt = TimestampConvertor.convertFromProtoTimestampToLocalDateTime(slot.getEndAt());

        String time = startAt.format(startAt.getMinute() == 0 ? startFormatter: startFormatterWithMin) + " - " + endAt.format(endAt.getMinute() == 0? endFormatter : endFormatterWithMin);

        // email to student
        MimeMessage studentMessage = mailSender.createMimeMessage();
        MimeMessageHelper studentMessageHelper = new MimeMessageHelper(studentMessage, true);

        studentMessageHelper.setFrom(new InternetAddress(senderEmail, "Slotify"));
        studentMessageHelper.setTo(student.getEmail());

        studentMessageHelper.setSubject("Your class with " + coachName + " @ " + time + " confirmed!");

        Map<String, Object> studentEmailVariables = new HashMap<>();
        studentEmailVariables.put("studentName", studentName);
        studentEmailVariables.put("coachName", coachName);
        studentEmailVariables.put("time", time);
        studentEmailVariables.put("websiteUrl", studentFrontendUrl);
        studentEmailVariables.put("cancelUrl", backendApiPrefix + "/" + slot.getId() + "/token/" + token + "?status=CANCELLED");

        Context studentEmailContext = new Context();
        studentEmailContext.setVariables(studentEmailVariables);
        String studentEmailContent = templateEngine.process("appointment-student", studentEmailContext);

        studentMessageHelper.setText(studentEmailContent, true);

        byte[] icsBytesForStudent = generateCalendarInvite(startAt, endAt, "Class with " + coachName, coach.getEmail(), coach.getName());
        ByteArrayResource resourceForStudent = new ByteArrayResource(icsBytesForStudent);
        studentMessageHelper.addAttachment("invite.ics", resourceForStudent);

        mailSender.send(studentMessage);
        log.info("Appointment email sent to student: {}", studentName);

        // email to coach
        MimeMessage coachMessage = mailSender.createMimeMessage();
        MimeMessageHelper coachMessageHelper = new MimeMessageHelper(coachMessage, true);

        coachMessageHelper.setFrom(new InternetAddress(senderEmail, "Slotify"));
        coachMessageHelper.setTo(coach.getEmail());
        coachMessageHelper.setSubject("Class Confirmed: " + studentName + " has confirmed your class @ " + time);

        Map<String, Object> coachEmailVariables = new HashMap<>();
        coachEmailVariables.put("studentName", studentName);
        coachEmailVariables.put("coachName", coachName);
        coachEmailVariables.put("time", time);
        coachEmailVariables.put("websiteUrl", coachFrontendUrl);
        coachEmailVariables.put("cancelUrl", backendApiPrefix + "/" + slot.getId() + "/token/" + token + "?status=CANCELLED");

        Context coachEmailContext = new Context();
        coachEmailContext.setVariables(coachEmailVariables);
        String coachEmailContent = templateEngine.process("appointment-coach", coachEmailContext);

        coachMessageHelper.setText(coachEmailContent, true);

        byte[] icsBytesForCoach = generateCalendarInvite(startAt, endAt, "Class with " + studentName, student.getEmail(), student.getName());
        ByteArrayResource resourceForCoach = new ByteArrayResource(icsBytesForCoach);
        coachMessageHelper.addAttachment("invite.ics", resourceForCoach);

        mailSender.send(coachMessage);
        log.info("Appointment email sent to coach: {}", coachName);
    }

    private void sendRejectedEmail(Slot slot) throws MessagingException, IOException {
        Student student = slot.getStudent();
        Coach coach = slot.getCoach();
        String studentName = WordUtils.capitalizeFully(student.getName().split(" ")[0].toLowerCase());
        String coachName = WordUtils.capitalizeFully(coach.getName().split(" ")[0].toLowerCase());

        LocalDateTime startAt = TimestampConvertor.convertFromProtoTimestampToLocalDateTime(slot.getStartAt());
        LocalDateTime endAt = TimestampConvertor.convertFromProtoTimestampToLocalDateTime(slot.getEndAt());
        String time = startAt.format(startAt.getMinute() == 0 ? startFormatter: startFormatterWithMin) + " - " + endAt.format(endAt.getMinute() == 0? endFormatter : endFormatterWithMin);

        // email to student
        MimeMessage studentMessage = mailSender.createMimeMessage();
        MimeMessageHelper studentMessageHelper = new MimeMessageHelper(studentMessage, true);

        studentMessageHelper.setFrom(new InternetAddress(senderEmail, "Slotify"));
        studentMessageHelper.setTo(student.getEmail());

        studentMessageHelper.setSubject("Your class with " + coachName + " @ " + time + " rejected!");

        Map<String, Object> studentEmailVariables = new HashMap<>();
        studentEmailVariables.put("studentName", studentName);
        studentEmailVariables.put("coachName", coachName);
        studentEmailVariables.put("time", time);
        studentEmailVariables.put("websiteUrl", studentFrontendUrl);

        Context studentEmailContext = new Context();
        studentEmailContext.setVariables(studentEmailVariables);
        String studentEmailContent = templateEngine.process("rejected-student", studentEmailContext);

        studentMessageHelper.setText(studentEmailContent, true);

        mailSender.send(studentMessage);
        log.info("Rejection email sent to student: {}", studentName);

        // email to coach
        MimeMessage coachMessage = mailSender.createMimeMessage();
        MimeMessageHelper coachMessageHelper = new MimeMessageHelper(coachMessage, true);

        coachMessageHelper.setFrom(new InternetAddress(senderEmail, "Slotify"));
        coachMessageHelper.setTo(coach.getEmail());
        coachMessageHelper.setSubject("Class Confirmed: " + studentName + " has rejected your class  @ " + time);

        Map<String, Object> coachEmailVariables = new HashMap<>();
        coachEmailVariables.put("studentName", studentName);
        coachEmailVariables.put("coachName", coachName);
        coachEmailVariables.put("time", time);

        Context coachEmailContext = new Context();
        coachEmailContext.setVariables(coachEmailVariables);
        String coachEmailContent = templateEngine.process("rejected-coach", coachEmailContext);

        coachMessageHelper.setText(coachEmailContent, true);

        mailSender.send(coachMessage);
        log.info("Rejection email sent to coach: {}", coachName);
    }

    private void sendCancelledEmail(Slot slot) throws MessagingException, IOException {
        Student student = slot.getStudent();
        Coach coach = slot.getCoach();
        String studentName = WordUtils.capitalizeFully(student.getName().split(" ")[0].toLowerCase());
        String coachName = WordUtils.capitalizeFully(coach.getName().split(" ")[0].toLowerCase());

        LocalDateTime startAt = TimestampConvertor.convertFromProtoTimestampToLocalDateTime(slot.getStartAt());
        LocalDateTime endAt = TimestampConvertor.convertFromProtoTimestampToLocalDateTime(slot.getEndAt());
        String time = startAt.format(startAt.getMinute() == 0 ? startFormatter: startFormatterWithMin) + " - " + endAt.format(endAt.getMinute() == 0? endFormatter : endFormatterWithMin);

        // email to student
        MimeMessage studentMessage = mailSender.createMimeMessage();
        MimeMessageHelper studentMessageHelper = new MimeMessageHelper(studentMessage, true);

        studentMessageHelper.setFrom(new InternetAddress(senderEmail, "Slotify"));
        studentMessageHelper.setTo(student.getEmail());

        studentMessageHelper.setSubject("Your class with " + coachName + " @ " + time + " cancelled!");

        Map<String, Object> studentEmailVariables = new HashMap<>();
        studentEmailVariables.put("studentName", studentName);
        studentEmailVariables.put("coachName", coachName);
        studentEmailVariables.put("time", time);

        Context studentEmailContext = new Context();
        studentEmailContext.setVariables(studentEmailVariables);
        String studentEmailContent = templateEngine.process("cancelled-student", studentEmailContext);

        studentMessageHelper.setText(studentEmailContent, true);

        mailSender.send(studentMessage);
        log.info("Cancellation email sent to student: {}", studentName);

        // email to coach
        MimeMessage coachMessage = mailSender.createMimeMessage();
        MimeMessageHelper coachMessageHelper = new MimeMessageHelper(coachMessage, true);

        coachMessageHelper.setFrom(new InternetAddress(senderEmail, "Slotify"));
        coachMessageHelper.setTo(coach.getEmail());
        coachMessageHelper.setSubject("Class Cancelled: " + studentName + " has cancelled your class  @ " + time);

        Map<String, Object> coachEmailVariables = new HashMap<>();
        coachEmailVariables.put("studentName", studentName);
        coachEmailVariables.put("coachName", coachName);
        coachEmailVariables.put("time", time);

        Context coachEmailContext = new Context();
        coachEmailContext.setVariables(coachEmailVariables);
        String coachEmailContent = templateEngine.process("cancelled-coach", coachEmailContext);

        coachMessageHelper.setText(coachEmailContent, true);

        mailSender.send(coachMessage);
        log.info("Cancellation email sent to coach: {}", coachName);
    }

    private byte[] generateCalendarInvite(LocalDateTime start, LocalDateTime end, String eventName, String email, String name) throws IOException {
        // Create a TimeZone
        TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
        net.fortuna.ical4j.model.TimeZone timezone = registry.getTimeZone("America/Los_Angeles");
        VTimeZone tz = timezone.getVTimeZone();

        VEvent meeting = new VEvent(start, end, eventName);
        // add timezone info..
        meeting.add(tz.getTimeZoneId());

        // generate unique identifier..
        UidGenerator ug = new RandomUidGenerator();
        meeting.add(ug.generateUid());

        // add attendees
        Attendee attendee = new Attendee(URI.create("mailto:" + email));
        attendee.add(Role.REQ_PARTICIPANT);
        attendee.add(new Cn(name));
        meeting.add(attendee);

        // add organizer
        Organizer organizer = new Organizer(URI.create("mailto:slotify.txy@gmail.com"));
        organizer.add(new Cn("Slotify"));
        meeting.add(organizer);

        net.fortuna.ical4j.model.Calendar calendar = new Calendar()
                .withProdId("-//Slotify//Slotify 1.0//EN")
                .withDefaults()
                .withComponent(meeting)
                .getFluentTarget();

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        CalendarOutputter outputter = new CalendarOutputter();
        outputter.output(calendar, bout);
        return bout.toByteArray();
    }
}

