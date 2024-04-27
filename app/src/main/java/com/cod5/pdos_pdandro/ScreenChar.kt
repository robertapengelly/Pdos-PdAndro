package com.cod5.pdos_pdandro

import android.graphics.Color

  /* a single character in our ANSI console */
class ScreenChar(str: String) {
    var txt = str
    var decoration = ""
    var color = Color.WHITE
    var bgcolor = Color.BLACK
    var typeface = Console.normal

    fun set(model: ScreenChar)
    {
        typeface = model.typeface
        color = model.color
        bgcolor = model.bgcolor
        decoration = model.decoration
    }
    fun reset() {
        color = Color.WHITE
        bgcolor = Color.BLACK
        typeface = Console.normal
        decoration = ""
    }
}