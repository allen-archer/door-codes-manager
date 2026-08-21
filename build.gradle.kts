plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.vaadin") version "25.2.6"
	kotlin("plugin.jpa") version "2.3.21"
}

group = "com.allenarcher"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

extra["vaadinVersion"] = "25.2.6"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	developmentOnly("com.vaadin:vaadin-dev")
	implementation("com.vaadin:vaadin-spring-boot-starter")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.integration:spring-integration-mqtt")
	implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
	runtimeOnly("org.xerial:sqlite-jdbc")
	runtimeOnly("org.hibernate.orm:hibernate-community-dialects")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
	testImplementation("org.mockito:mockito-junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("com.vaadin:vaadin-bom:${property("vaadinVersion")}")
	}
}

val mockitoAgent = configurations.create("mockitoAgent")
dependencies {
	mockitoAgent("org.mockito:mockito-core") { isTransitive = false }
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
	systemProperty("vaadin.copilot.enable", "false")
}

if (gradle.startParameter.taskNames.any { it == "buildProduction" }) {
	extra["vaadin.productionMode"] = ""
}

tasks.register("buildProduction") {
	group = "build"
	description = "Builds a bootJar with an optimized, minified Vaadin production frontend bundle"
	dependsOn("build")
}

tasks.named<Test>("test") {
	jvmArgs("-javaagent:${mockitoAgent.asPath}")
}
