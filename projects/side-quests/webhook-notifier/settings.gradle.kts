pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
	}
	plugins {
		id("org.springframework.boot") version "4.1.1"
		id("io.spring.dependency-management") version "1.1.7"
	}
}

rootProject.name = "webhook-notifier"

include("webhook", "worker", "fanout")
