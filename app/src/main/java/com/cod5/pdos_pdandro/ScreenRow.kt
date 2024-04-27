package com.cod5.pdos_pdandro

  /* a row in our ANSI console */
class ScreenRow (nbcol: Int) {
    private var col = arrayListOf<ScreenChar>()
    init {
        var i = 0
        while (i < nbcol) {
            col.add(ScreenChar(" "))
            i++
        }
    }
    fun set(model: ScreenChar) {
        var i = 0
        while (i < col.size) {
            col[i].set(model)
            i++
        }
    }
    operator fun get(j: Int): ScreenChar {
        return col[j]
    }
}
