package com.allenarcher.door_codes_manager.ui

import com.allenarcher.door_codes_manager.database.DeviceRepository
import com.allenarcher.door_codes_manager.database.Device
import com.allenarcher.door_codes_manager.database.DoorCodeRepository
import com.allenarcher.door_codes_manager.database.DoorCode
import com.allenarcher.door_codes_manager.state.StateManager
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.page.ColorScheme
import com.vaadin.flow.component.progressbar.ProgressBar
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.Tabs
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route("")
@PermitAll
class MainView(
    private val doorCodeRepository: DoorCodeRepository,
    private val deviceRepository: DeviceRepository,
    private val stateManager: StateManager,
) : VerticalLayout() {

    private val doorCodesGrid = Grid(DoorCode::class.java, false)
    private val devicesGrid = Grid(Device::class.java, false)
    private var darkMode = true
    private val themeToggle = Button(Icon(VaadinIcon.SUN_O)) { toggleTheme() }

    private val doorCodesTab = Tab("Door Codes")
    private val devicesTab = Tab("Devices")

    init {
        width = "100%"
        val header = HorizontalLayout(H1("#️⃣ Door Codes Manager"), themeToggle)
        header.width = "100%"
        header.style.set("flex-wrap", "wrap")
        add(header)
        val doorCodesActions = HorizontalLayout(
            Button(Icon(VaadinIcon.PLUS)) { openCodeDialog(null) }.apply { setTooltipText("Add code") },
            Button(Icon(VaadinIcon.REFRESH)) { openSyncDialog() }.apply { setTooltipText("Read codes from a device") },
        )
        val devicesActions = HorizontalLayout(
            Button(Icon(VaadinIcon.PLUS)) { openDeviceDialog() }.apply { setTooltipText("Add device") },
        )
        devicesActions.isVisible = false
        val tabs = Tabs(doorCodesTab, devicesTab)
        tabs.addSelectedChangeListener {
            doorCodesGrid.isVisible = tabs.selectedTab == doorCodesTab
            devicesGrid.isVisible = tabs.selectedTab == devicesTab
            doorCodesActions.isVisible = tabs.selectedTab == doorCodesTab
            devicesActions.isVisible = tabs.selectedTab == devicesTab
        }
        val toolbar = HorizontalLayout(tabs, doorCodesActions, devicesActions)
        toolbar.width = "100%"
        toolbar.style.set("flex-wrap", "wrap")
        toolbar.alignItems = FlexComponent.Alignment.CENTER
        add(toolbar)
        configureDoorCodesGrid()
        configureDeviceGrid()
        add(doorCodesGrid, devicesGrid)
        devicesGrid.isVisible = false
        refreshGrid()
        refreshDeviceGrid()
    }

    private fun toggleTheme() {
        darkMode = !darkMode
        UI.getCurrent().page.colorScheme = if (darkMode) ColorScheme.Value.DARK else ColorScheme.Value.LIGHT
        themeToggle.icon = Icon(if (darkMode) VaadinIcon.SUN_O else VaadinIcon.MOON_O)
    }

    private fun configureDoorCodesGrid() {
        doorCodesGrid.addColumn { it.device.name }.setHeader("Device")
        doorCodesGrid.addColumn { it.slot }.setHeader("Slot")
        doorCodesGrid.addColumn { it.code }.setHeader("Code")
        doorCodesGrid.addColumn { it.description ?: "" }.setHeader("Description")
        doorCodesGrid.addColumn { it.startDate?.toString() ?: "" }.setHeader("Starts")
        doorCodesGrid.addColumn { it.expirationDate?.toString() ?: "" }.setHeader("Expires")
        doorCodesGrid.addItemClickListener { openCodeDialog(it.item) }
    }

    private fun refreshGrid() {
        doorCodesGrid.setItems(doorCodeRepository.findAllByOrderBySlot())
    }

    private fun configureDeviceGrid() {
        devicesGrid.addColumn { it.name }.setHeader("Name")
        devicesGrid.addColumn { it.friendlyName }.setHeader("Friendly name")
        devicesGrid.addColumn { it.slotMin }.setHeader("Slot min")
        devicesGrid.addColumn { it.slotMax }.setHeader("Slot max")
        devicesGrid.addColumn { it.automatedSlotStart }.setHeader("Automated slot start")
        devicesGrid.addItemClickListener { openDeviceDialog(it.item) }
    }

    private fun refreshDeviceGrid() {
        devicesGrid.setItems(deviceRepository.findAll())
    }

    private fun deleteCode(doorCode: DoorCode) {
        if (!stateManager.deleteDoorCode(doorCode)) {
            Notification.show("This failed for unknown reasons. You can try again if you want.")
            return
        }
        refreshGrid()
    }

    private fun syncFromDevices(device: Device, start: Int, end: Int, progressBar: ProgressBar) {
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
        refreshGrid()
        Thread.sleep(750)
    }

    private fun addCloseButton(dialog: Dialog) {
        dialog.header.add(Button(Icon(VaadinIcon.CLOSE_SMALL)) { dialog.close() })
        dialog.width = "min(90vw, 600px)"
    }

    private fun openSyncDialog() {
        val dialog = Dialog("Read codes from device")
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
            syncFromDevices(device.value, start.value, end.value, progressBar)
            dialog.close()
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
        addCloseButton(dialog)
        val form = FormLayout(device, start, end)
        form.setColspan(device, 2)
        dialog.add(heading, form, buttons, progressBar)
        dialog.open()
    }

    private fun openDeviceDialog(existing: Device? = null) {
        val dialog = Dialog(if (existing == null) "Add device" else "Edit device")
        addCloseButton(dialog)
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
            dialog.close()
            refreshDeviceGrid()
        }
        val delete = Button("Delete") {
            deleteDevice(existing!!)
        }
        delete.isVisible = existing != null
        val buttons = HorizontalLayout(save, delete)
        buttons.width = "100%"
        buttons.justifyContentMode = FlexComponent.JustifyContentMode.END
        buttons.style.set("margin-top", "1em")
        dialog.add(FormLayout(name, friendlyName, slotMin, slotMax, automatedSlotStart), buttons)
        dialog.open()
    }

    private fun deleteDevice(device: Device) {
        stateManager.deleteDevice(device)
    }

    private fun openCodeDialog(existing: DoorCode?) {
        val dialog = Dialog(if (existing == null) "Add code" else "Edit code")
        addCloseButton(dialog)
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
            dialog.close()
            refreshGrid()
        }
        val delete = Button("Delete") {
            deleteCode(existing!!)
        }
        delete.isVisible = existing != null
        val buttons = HorizontalLayout(save, delete)
        buttons.width = "100%"
        buttons.justifyContentMode = FlexComponent.JustifyContentMode.END
        buttons.style.set("margin-top", "1em")
        val form = FormLayout(device, slot, code, description, startDate, expirationDate)
        form.setColspan(device, 2)
        form.setColspan(description, 2)
        dialog.add(form, buttons)
        dialog.open()
    }
}