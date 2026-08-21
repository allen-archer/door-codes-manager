package com.allenarcher.door_codes_manager

import com.vaadin.flow.component.dependency.StyleSheet
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.ColorScheme
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.shared.communication.PushMode
import com.vaadin.flow.theme.aura.Aura
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
@StyleSheet(Aura.STYLESHEET)
@ColorScheme(ColorScheme.Value.DARK)
@Push(PushMode.MANUAL)
class DoorCodesManagerApplication : AppShellConfigurator

fun main(args: Array<String>) {
	runApplication<DoorCodesManagerApplication>(*args)
}
