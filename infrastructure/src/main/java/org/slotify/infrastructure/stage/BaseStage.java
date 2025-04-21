package org.slotify.infrastructure.stage;
import org.slotify.infrastructure.ServiceInfo;
import software.amazon.awscdk.Stage;
import software.amazon.awscdk.StageProps;
import software.constructs.Construct;

public abstract class BaseStage extends Stage {
    final ServiceInfo apiGatewayServiceInfo = new ServiceInfo("api-gateway", 8084, null, null);
    final ServiceInfo authServiceInfo = new ServiceInfo("auth-service", 8082, null, null);
    final ServiceInfo userServiceInfo = new ServiceInfo("user-service", 8081, 9001, "userDB");
    final ServiceInfo slotServiceInfo = new ServiceInfo("slot-service", 8080, 9002, "slotDb");
    final ServiceInfo openHourServiceInfo = new ServiceInfo("open-hour-service", 8083, null, "openHourDb");
    final ServiceInfo emailTokenServiceInfo = new ServiceInfo("email-token-service", 8086, 9003, "emailTokenDb");
    final ServiceInfo notificationServiceInfo = new ServiceInfo("notification-service", 8085, null, null);

    public BaseStage(final Construct scope, final String id, final StageProps props) {
        super(scope, id, props);
    }
}
