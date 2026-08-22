package com.allenarcher.door_codes_manager.ui.home

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon

fun addCloseButton(dialog: Dialog) {
    dialog.header.add(Button(Icon(VaadinIcon.CLOSE_SMALL)) { dialog.close() })
    dialog.width = "min(90vw, 600px)"
}