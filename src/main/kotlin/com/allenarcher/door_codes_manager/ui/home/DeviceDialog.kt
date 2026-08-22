package com.allenarcher.door_codes_manager.ui.home

import com.allenarcher.door_codes_manager.database.Device
import com.allenarcher.door_codes_manager.database.DeviceRepository
import com.allenarcher.door_codes_manager.state.StateManager
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField

class DeviceDialog(
    private val deviceRepository: DeviceRepository,
    private val stateManager: StateManager,
    existing: Device?,
    private val onSaved: () -> Unit,
) : Dialog(if (existing == null) "Add device" else "Edit device") {
    init {
        addCloseButton(this)
        val name = TextField("Name")
        val friendlyName = TextField("Friendly name")
        val slotMin = IntegerField("Slot min")
        val slotMax = IntegerField("Slot max")
        val automatedSlotStart = IntegerField("Automated Slot Start")
        if (existing != null) {
            name.value = existing.name
            friendlyName.value = existing.friendlyName
            slotMin.value = existing.slotMin
            slotMax.value = existing.slotMax
            automatedSlotStart.value = existing.automatedSlotStart
        }
        val save = Button("Save") {
            if (name.isEmpty || friendlyName.isEmpty || slotMin.isEmpty || slotMax.isEmpty) {
                Notification.show("All fields are required")
                return@Button
            }
            val device = existing ?: Device(
                name = name.value,
                friendlyName = friendlyName.value,
                slotMin = slotMin.value,
                slotMax = slotMax.value,
                automatedSlotStart = automatedSlotStart.value
            )
            if (existing != null) {
                device.name = name.value
                device.friendlyName = friendlyName.value
                device.slotMin = slotMin.value
                device.slotMax = slotMax.value
                device.automatedSlotStart = automatedSlotStart.value
            }
            deviceRepository.save(device)
            close()
            onSaved()
        }
        val delete = Button("Delete") {
            stateManager.deleteDevice(existing!!)
            close()
            onSaved()
        }
        delete.isVisible = existing != null
        val buttons = HorizontalLayout(save, delete)
        buttons.width = "100%"
        buttons.justifyContentMode = FlexComponent.JustifyContentMode.END
        buttons.style.set("margin-top", "1em")
        add(FormLayout(name, friendlyName, slotMin, slotMax, automatedSlotStart), buttons)
    }
}