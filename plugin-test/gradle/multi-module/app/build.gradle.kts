plugins {
    id("application")
    id("multi-project.conventions")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
//    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
//    implementation 'org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j'
//    compileOnly 'org.projectlombok:lombok'
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation(project(":module1-spring-only"))
    implementation(project(":module2-additional-metadata"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
