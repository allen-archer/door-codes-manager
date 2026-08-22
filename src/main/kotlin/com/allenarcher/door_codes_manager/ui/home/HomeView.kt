package com.allenarcher.door_codes_manager.ui.home

import com.allenarcher.door_codes_manager.database.DeviceRepository
import com.allenarcher.door_codes_manager.database.DoorCodeRepository
import com.allenarcher.door_codes_manager.state.StateManager
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.page.ColorScheme
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.Tabs
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route("")
@PermitAll
class HomeView(
    doorCodeRepository: DoorCodeRepository,
    deviceRepository: DeviceRepository,
    stateManager: StateManager,
) : VerticalLayout() {
    private var darkMode = true
    private val themeToggle = Button(Icon(VaadinIcon.SUN_O)) { toggleTheme() }
    private val doorCodesTab = Tab("Door Codes")
    private val devicesTab = Tab("Devices")
    private val doorCodes = DoorCodesTab(doorCodeRepository, deviceRepository, stateManager)
    private val devices = DevicesTab(deviceRepository, stateManager)

    init {
        width = "100%"
        val header = HorizontalLayout(H1("#️⃣ Door Codes Manager"), themeToggle)
        header.width = "100%"
        header.style.set("flex-wrap", "wrap")
        add(header)
        devices.isVisible = false
        devices.actions.isVisible = false
        val tabs = Tabs(doorCodesTab, devicesTab)
        tabs.addSelectedChangeListener {
            doorCodes.isVisible = tabs.selectedTab == doorCodesTab
            devices.isVisible = tabs.selectedTab == devicesTab
            doorCodes.actions.isVisible = tabs.selectedTab == doorCodesTab
            devices.actions.isVisible = tabs.selectedTab == devicesTab
        }
        val toolbar = HorizontalLayout(tabs, doorCodes.actions, devices.actions)
        toolbar.width = "100%"
        toolbar.style.set("flex-wrap", "wrap")
        toolbar.alignItems = FlexComponent.Alignment.CENTER
        add(toolbar)
        add(doorCodes, devices)
    }

    private fun toggleTheme() {
        darkMode = !darkMode
        UI.getCurrent().page.colorScheme = if (darkMode) ColorScheme.Value.DARK else ColorScheme.Value.LIGHT
        themeToggle.icon = Icon(if (darkMode) VaadinIcon.SUN_O else VaadinIcon.MOON_O)
    }
}