package com.allenarcher.door_codes_manager.ui.home

import com.allenarcher.door_codes_manager.database.Device
import com.allenarcher.door_codes_manager.database.DeviceRepository
import com.allenarcher.door_codes_manager.state.StateManager
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout

class DevicesTab(
    private val deviceRepository: DeviceRepository,
    private val stateManager: StateManager,
) : VerticalLayout() {
    private val grid = Grid(Device::class.java, false)
    val actions = HorizontalLayout(
        Button(Icon(VaadinIcon.PLUS)) { openDeviceDialog(null) }.apply { setTooltipText("Add device") },
    )

    init {
        isPadding = false
        grid.addColumn { it.name }.setHeader("Name")
        grid.addColumn { it.friendlyName }.setHeader("Friendly name")
        grid.addColumn { it.slotMin }.setHeader("Slot min")
        grid.addColumn { it.slotMax }.setHeader("Slot max")
        grid.addColumn { it.automatedSlotStart }.setHeader("Automated slot start")
        grid.addItemClickListener { openDeviceDialog(it.item) }
        add(grid)
        refresh()
    }

    fun refresh() {
        grid.setItems(deviceRepository.findAll())
    }

    private fun openDeviceDialog(existing: Device?) {
        DeviceDialog(deviceRepository, stateManager, existing) { refresh() }.open()
    }
}