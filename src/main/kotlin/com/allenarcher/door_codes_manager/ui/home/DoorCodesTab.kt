package com.allenarcher.door_codes_manager.ui.home

import com.allenarcher.door_codes_manager.database.DeviceRepository
import com.allenarcher.door_codes_manager.database.DoorCode
import com.allenarcher.door_codes_manager.database.DoorCodeRepository
import com.allenarcher.door_codes_manager.state.StateManager
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import java.time.format.DateTimeFormatter

private val DATE_FORMAT = DateTimeFormatter.ofPattern("M/d/yy H:mm")

class DoorCodesTab(
    private val doorCodeRepository: DoorCodeRepository,
    private val deviceRepository: DeviceRepository,
    private val stateManager: StateManager,
) : VerticalLayout() {
    private val grid = Grid(DoorCode::class.java, false)
    val actions = HorizontalLayout(
        Button(Icon(VaadinIcon.PLUS)) { openCodeDialog(null) }.apply { setTooltipText("Add code") },
        Button(Icon(VaadinIcon.REFRESH)) { openSyncDialog() }.apply { setTooltipText("Read codes from a device") },
    )

    init {
        isPadding = false
        grid.addColumn { it.description ?: "" }.setHeader("Description").setAutoWidth(true)
        grid.addColumn { it.code }.setHeader("Code").setAutoWidth(true)
        grid.addColumn { it.slot }.setHeader("Slot").setAutoWidth(true)
        grid.addColumn { it.device.name }.setHeader("Device").setAutoWidth(true)
        grid.addColumn { it.startDate?.format(DATE_FORMAT) ?: "" }.setHeader("Starts").setAutoWidth(true)
        grid.addColumn { it.expirationDate?.format(DATE_FORMAT) ?: "" }.setHeader("Expires").setAutoWidth(true)
        grid.addItemClickListener { openCodeDialog(it.item) }
        add(grid)
        refresh()
    }

    fun refresh() {
        grid.setItems(doorCodeRepository.findAllByOrderBySlot())
    }

    private fun openCodeDialog(existing: DoorCode?) {
        CodeDialog(doorCodeRepository, deviceRepository, stateManager, existing) { refresh() }.open()
    }

    private fun openSyncDialog() {
        SyncDialog(deviceRepository, stateManager) { refresh() }.open()
    }
}