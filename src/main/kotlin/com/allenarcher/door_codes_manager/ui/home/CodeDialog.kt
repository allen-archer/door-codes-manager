package com.allenarcher.door_codes_manager.ui.home

import com.allenarcher.door_codes_manager.database.Device
import com.allenarcher.door_codes_manager.database.DeviceRepository
import com.allenarcher.door_codes_manager.database.DoorCode
import com.allenarcher.door_codes_manager.database.DoorCodeRepository
import com.allenarcher.door_codes_manager.state.StateManager
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField

class CodeDialog(
    private val doorCodeRepository: DoorCodeRepository,
    private val deviceRepository: DeviceRepository,
    private val stateManager: StateManager,
    existing: DoorCode?,
    private val onSaved: () -> Unit,
) : Dialog(if (existing == null) "Add code" else "Edit code") {
    init {
        addCloseButton(this)
        val device = ComboBox<Device>("Device")
        val allDevices = deviceRepository.findAll()
        device.setItems(allDevices)
        device.setItemLabelGenerator { it.name }
        val slot = IntegerField("Slot")
        val code = TextField("Code")
        val description = TextField("Description")
        val startDate = DateTimePicker("Starts")
        val expirationDate = DateTimePicker("Expires")
        if (existing != null) {
            device.value = existing.device
            device.isEnabled = false
            slot.value = existing.slot
            slot.isEnabled = false
            code.value = existing.code
            description.value = existing.description ?: ""
            startDate.value = existing.startDate
            startDate.isEnabled = existing.startDate != null
            expirationDate.value = existing.expirationDate
        } else {
            device.value = allDevices.first()
            val updateSlot = {
                val usedSlots = doorCodeRepository.findAllByDevice_Name(device.value.name).map { it.slot }
                slot.value = (device.value.slotMin..device.value.slotMax).firstOrNull { it !in usedSlots }
            }
            updateSlot()
            device.addValueChangeListener { updateSlot() }
        }
        val save = Button("Save") {
            if (device.isEmpty || slot.isEmpty || code.isEmpty) {
                Notification.show("Device, slot, and code are required")
                return@Button
            }
            val deviceAndSlot = doorCodeRepository.findByDevice_NameAndSlot(device.value.name, slot.value)
            if (deviceAndSlot != null && deviceAndSlot.id != existing?.id) {
                Notification.show("That slot is already in use on this device, edit it to change the code")
                return@Button
            }
            val duplicateCode = doorCodeRepository.findByDevice_NameAndCode(device.value.name, code.value)
            if (duplicateCode != null && duplicateCode.id != existing?.id) {
                Notification.show("That code is already in use on this device")
                return@Button
            }
            val newDoorCode = DoorCode(
                null,
                device.value,
                slot.value,
                code.value,
                description.value,
                startDate.value,
                expirationDate.value
            )
            val confirmed = if (existing != null) {
                newDoorCode.id = existing.id
                stateManager.updateDoorCode(newDoorCode, existing)
            } else {
                stateManager.addDoorCode(newDoorCode)
            }
            if (!confirmed) {
                Notification.show("This failed for unknown reasons. You can try again if you want.")
                return@Button
            }
            close()
            onSaved()
        }
        val delete = Button("Delete") {
            if (!stateManager.deleteDoorCode(existing!!)) {
                Notification.show("This failed for unknown reasons. You can try again if you want.")
                return@Button
            }
            close()
            onSaved()
        }
        delete.isVisible = existing != null
        val buttons = HorizontalLayout(save, delete)
        buttons.width = "100%"
        buttons.justifyContentMode = FlexComponent.JustifyContentMode.END
        buttons.style.set("margin-top", "1em")
        val form = FormLayout(device, slot, code, description, startDate, expirationDate)
        form.setColspan(device, 2)
        form.setColspan(description, 2)
        add(form, buttons)
    }
}