# Slotify

The project is to streamline the class booking process and maximize the booking utilization. 

Consider this situation where the class scheduling is done via messaging: Student A books a class at 3 pm tomorrow and then Student B would like to have class as well and can only do 3 pm tomorrow. Then the coach askes A if they can pick a different time and A does have other availbility. They both have class, but it comes at a cost of communication. This is what the project is solving.

The project contains 3 parts, two frontends, one for the coaches, one for the students, and a backend.

This repository contains the microservices‐based implementation.

Other repos:
* [Monolithic-based implementation](https://github.com/XiyuanTu/slotify_backend_monolithic)
* [Frontend for Students](https://github.com/Slotify-txy/frontend_student)
* [Frontend for Coaches](https://github.com/Slotify-txy/frontend_coach)

## Features
* Students can submit availbility by selecting time slots and confirm/reject/accept/cancel appointments.
* Coaches can publish open hours, schedule classes based on students' availbility, and confirm/reject/accept/cancel appointments. 
* The auto scheduling feature will let coaches schedule as many as classes as possible.
* Email notification for open hour and slot status update, with action buttons for accept/confirm/reject/cancel and calendar invites

## Built With
* ![Spring Boot][Spring Boot]
* ![Hibernate][Hibernate]
* ![React][React.js]
* ![Redux][Redux]
* ![AWS][AWS]

## Known Issues
Having a hard time gaining production assess for AWS SES, so only verified email accounts can receive emails. Considering going back to Gmail SMTP.

## Contact

Xiyuan Tu - [LinkedIn](https://www.linkedin.com/in/xiyuan) - xiyuan.tyler@gmail.com

Looking for new-grad sde positions (backend/full-stack/cloud/iOS)

[Spring Boot]: https://img.shields.io/badge/Spring_Boot-brightgreen?style=for-the-badge&logo=springboot&logoColor=white
[Hibernate]: https://img.shields.io/badge/Hibernate-4A4A55?style=for-the-badge&logo=hibernate&logoColor=white
[React.js]: https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB
[Redux]: https://img.shields.io/badge/Redux-%23764ABC?style=for-the-badge&logo=redux&logoColor=white
[AWS]: https://img.shields.io/badge/AWS-131f2d?style=for-the-badge&logo=amazonwebservices&logoColor=white

