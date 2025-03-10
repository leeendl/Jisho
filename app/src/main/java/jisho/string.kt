package jisho

val EtoH = mapOf(
    "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お",
    "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
    "sa" to "さ", "shi" to "し", "su" to "す", "se" to "せ", "so" to "そ",
    "ta" to "た", "chi" to "ち", "tsu" to "つ", "te" to "て", "to" to "と",
    "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
    "ha" to "は", "hi" to "ひ", "fu" to "ふ", "he" to "へ", "ho" to "ほ",
    "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
    "ya" to "や", "yu" to "ゆ", "yo" to "よ",
    "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
    "wa" to "わ", "wo" to "を", "n" to "ん",
    "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
    "za" to "ざ", "ji" to "じ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
    "da" to "だ", "di" to "ぢ", "du" to "づ", "de" to "で", "do" to "ど",
    "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
    "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
    "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ",
    "sha" to "しゃ", "shu" to "しゅ", "sho" to "しょ",
    "cha" to "ちゃ", "chu" to "ちゅ", "cho" to "ちょ",
    "nya" to "にゃ", "nyu" to "にゅ", "nyo" to "にょ",
    "hya" to "ひゃ", "hyu" to "ひゅ", "hyo" to "ひょ",
    "mya" to "みゃ", "myu" to "みゅ", "myo" to "みょ",
    "rya" to "りゃ", "ryu" to "りゅ", "ryo" to "りょ",
    "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",
    "ja" to "じゃ", "ju" to "じゅ", "jo" to "じょ",
    "bya" to "びゃ", "byu" to "びゅ", "byo" to "びょ",
    "pya" to "ぴゃ", "pyu" to "ぴゅ", "pyo" to "ぴょ",
    "kka" to "っか", "kke" to "っけ", "kki" to "っき", "kko" to "っこ", "kku" to "っく",
    "ssa" to "っさ", "sse" to "っせ", "sshi" to "っし", "sso" to "っそ", "ssu" to "っす",
    "tta" to "った", "tte" to "って", "tti" to "っち", "tto" to "っと", "ttu" to "っつ",
    "ppa" to "っぱ", "ppe" to "っぺ", "ppi" to "っぴ", "ppo" to "っぽ", "ppu" to "っぷ",
    "ssha" to "っしゃ", "sshu" to "っしゅ", "ssho" to "っしょ",
    "aa" to "あー", "ii" to "いー", "uu" to "うー", "ee" to "えー", "oo" to "おー"
)

val EtoHRegex = Regex("""(${EtoH.keys.sortedByDescending { it.length }.joinToString("|")})""")

/**
 * e.g. kara -> から
 */
fun replaceEtoH(english: String): String {
    return english.replace(EtoHRegex) {
        EtoH[it.value] ?: it.value
    }
}

/**
 * checks the string for invalid romaji e.g. karas -> からs (invalid.)
 */
fun String.canEtoH(): Boolean {
    var remaining = this
    while (remaining.isNotEmpty()) {
        val match = EtoH.keys
            .filter { remaining.startsWith(it) }
            .maxByOrNull { it.length }
            ?: return false
        remaining = remaining.removePrefix(match)
    }
    return true
}