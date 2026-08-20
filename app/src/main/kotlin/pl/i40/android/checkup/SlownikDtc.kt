package pl.i40.android.checkup

object SlownikDtc {
    fun zJson(json: String): Map<String, String> {
        val wpis = Regex("\"(P[0-9A-Z]+)\":\\s*\"([^\"]*)\"")
        return wpis.findAll(json).associate { it.groupValues[1] to it.groupValues[2] }
    }
}
