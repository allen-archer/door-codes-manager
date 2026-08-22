package com.allenarcher.door_codes_manager.ui.home

import com.allenarcher.door_codes_manager.database.Device
import com.allenarcher.door_codes_manager.database.DeviceRepository
import com.allenarcher.door_codes_manager.state.StateManager
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.progressbar.ProgressBar
import com.vaadin.flow.component.textfield.IntegerField

class SyncDialog(
    private val deviceRepository: DeviceRepository,
    private val stateManager: StateManager,
    private val onSynced: () -> Unit,
) : Dialog("Read codes from device") {
    init {
        addCloseButton(this)
        val heading = Paragraph("Reads the codes currently programmed on the device, from the start slot to the end slot, and saves them to the database")
        val start = IntegerField("Start slot")
        val end = IntegerField("End slot")
        val device = ComboBox<Device>("Device")
        val progressBar = ProgressBar()
        progressBar.isVisible = false
        val sync = Button("Read and save")
        sync.addClickListener {
            sync.isEnabled = false
            progressBar.value = 0.0
            progressBar.isVisible = true
            syncFromDevice(device.value, start.value, end.value, progressBar)
            close()
        }
        val devices = deviceRepository.findAll()
        device.setItems(devices)
        device.value = devices.first()
        device.setItemLabelGenerator { it.name }
        val updateStartAndEnd = {
            start.value = device.value.slotMin
            end.value = device.value.slotMax
        }
        updateStartAndEnd()
        device.addValueChangeListener { updateStartAndEnd() }
        val buttons = HorizontalLayout(sync)
        buttons.width = "100%"
        buttons.justifyContentMode = FlexComponent.JustifyContentMode.END
        buttons.style.set("margin-top", "1em")
        val form = FormLayout(device, start, end)
        form.setColspan(device, 2)
        add(heading, form, buttons, progressBar)
    }

    private fun syncFromDevice(device: Device, start: Int, end: Int, progressBar: ProgressBar) {
        val ui = UI.getCurrent()
        val total = end - start + 1
        val updates = stateManager.syncDoorCodes(
            device, start, end,
            onSlotSynced = { slot ->
                progressBar.value = (slot - start + 1).toDouble() / total
                ui.push()
            },
            onMessage = { message ->
                Notification.show(message, 10000, Notification.Position.BOTTOM_START)
                ui.push()
            },
        )
        if (!updates) Notification.show("Sync finished, but no updates were made.", 10000, Notification.Position.BOTTOM_START)
        onSynced()
        Thread.sleep(750)
    }
}