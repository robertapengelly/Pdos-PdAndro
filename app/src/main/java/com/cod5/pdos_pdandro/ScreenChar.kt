package com.cod5.pdos_pdandro

import  android.graphics.Color

class ScreenChar (str: String) {

    var typeface = Console.normal
    var txt = str
    
    var decoration = ""
    
    var backgroundColor = Color.BLACK
    var foregroundColor = Color.WHITE
    
    fun set (model: ScreenChar) {
    
        decoration = model.decoration
        typeface = model.typeface
        
        backgroundColor = model.backgroundColor
        foregroundColor = model.foregroundColor
    
    }
    
    fun reset () {
    
        decoration = ""
        typeface = Console.normal
        
        backgroundColor = Color.BLACK
        foregroundColor = Color.WHITE
    
    }

}