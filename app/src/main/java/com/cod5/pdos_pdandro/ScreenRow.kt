package com.cod5.pdos_pdandro

class ScreenRow (nbcol: Int) {

    private var col = arrayListOf<ScreenChar> ()
    
    init {
    
        for (i in 0 until nbcol) {
            col.add (ScreenChar (" "))
        }
    
    }
    
    fun set (model: ScreenChar) {
    
        for (i in 0 until col.size) {
            col[i].set (model)
        }
    
    }
    
    operator fun get (j: Int): ScreenChar {
        return col[j]
    }

}