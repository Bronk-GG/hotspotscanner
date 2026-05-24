package com.example.hotspotmonitor.data

/**
 * Offline IEEE OUI database subset.
 * Maps the first 3 bytes of a MAC address (OUI prefix) to a vendor name.
 * A real production app would bundle the full ~35k entry database.
 * This covers the most common consumer device vendors.
 */
object OuiDatabase {

    private val entries = mapOf(
        // Apple
        "00:03:93" to "Apple Inc.", "00:0a:27" to "Apple Inc.", "00:0a:95" to "Apple Inc.",
        "00:11:24" to "Apple Inc.", "00:14:51" to "Apple Inc.", "00:16:cb" to "Apple Inc.",
        "00:17:f2" to "Apple Inc.", "00:19:e3" to "Apple Inc.", "00:1b:63" to "Apple Inc.",
        "00:1c:b3" to "Apple Inc.", "00:1d:4f" to "Apple Inc.", "00:1e:52" to "Apple Inc.",
        "00:1e:c2" to "Apple Inc.", "00:1f:5b" to "Apple Inc.", "00:1f:f3" to "Apple Inc.",
        "00:21:e9" to "Apple Inc.", "00:22:41" to "Apple Inc.", "00:23:12" to "Apple Inc.",
        "00:23:32" to "Apple Inc.", "00:23:6c" to "Apple Inc.", "00:23:df" to "Apple Inc.",
        "00:24:36" to "Apple Inc.", "00:25:00" to "Apple Inc.", "00:25:4b" to "Apple Inc.",
        "00:25:bc" to "Apple Inc.", "00:26:08" to "Apple Inc.", "00:26:4a" to "Apple Inc.",
        "00:26:b9" to "Apple Inc.", "00:26:bb" to "Apple Inc.", "00:30:65" to "Apple Inc.",
        "04:0c:ce" to "Apple Inc.", "04:15:52" to "Apple Inc.", "04:1e:64" to "Apple Inc.",
        "04:26:65" to "Apple Inc.", "04:54:53" to "Apple Inc.", "04:f7:e4" to "Apple Inc.",
        "08:00:07" to "Apple Inc.", "08:6d:41" to "Apple Inc.", "0c:3e:9f" to "Apple Inc.",
        "0c:74:c2" to "Apple Inc.", "10:40:f3" to "Apple Inc.", "10:93:e9" to "Apple Inc.",
        "14:5a:05" to "Apple Inc.", "18:34:51" to "Apple Inc.", "18:9e:fc" to "Apple Inc.",
        "1c:36:bb" to "Apple Inc.", "20:78:f0" to "Apple Inc.", "28:cf:da" to "Apple Inc.",
        "28:e0:2c" to "Apple Inc.", "2c:61:f6" to "Apple Inc.", "34:15:9e" to "Apple Inc.",
        "38:c9:86" to "Apple Inc.", "3c:07:54" to "Apple Inc.", "40:30:04" to "Apple Inc.",
        "40:98:ad" to "Apple Inc.", "44:00:10" to "Apple Inc.", "44:fb:42" to "Apple Inc.",
        "48:60:bc" to "Apple Inc.", "50:32:75" to "Apple Inc.", "54:26:96" to "Apple Inc.",
        "58:1f:aa" to "Apple Inc.", "5c:96:9d" to "Apple Inc.", "60:03:08" to "Apple Inc.",
        "60:f8:1d" to "Apple Inc.", "64:5a:04" to "Apple Inc.", "68:a8:6d" to "Apple Inc.",
        "6c:40:08" to "Apple Inc.", "70:56:81" to "Apple Inc.", "74:e1:b6" to "Apple Inc.",
        "78:31:c1" to "Apple Inc.", "7c:6d:62" to "Apple Inc.", "80:be:05" to "Apple Inc.",
        "84:78:8b" to "Apple Inc.", "88:19:08" to "Apple Inc.", "8c:7b:9d" to "Apple Inc.",
        "90:27:e4" to "Apple Inc.", "90:b0:ed" to "Apple Inc.", "94:94:26" to "Apple Inc.",
        "98:d6:bb" to "Apple Inc.", "9c:f3:87" to "Apple Inc.", "a0:ed:cd" to "Apple Inc.",
        "a4:b1:97" to "Apple Inc.", "a4:c3:f0" to "Apple Inc.", "a4:d1:d2" to "Apple Inc.",
        "a8:20:66" to "Apple Inc.", "a8:5c:2c" to "Apple Inc.", "ac:3c:0b" to "Apple Inc.",
        "ac:87:a3" to "Apple Inc.", "b0:65:bd" to "Apple Inc.", "b4:f0:ab" to "Apple Inc.",
        "b8:8d:12" to "Apple Inc.", "bc:52:b7" to "Apple Inc.", "c0:84:7a" to "Apple Inc.",
        "c4:2c:03" to "Apple Inc.", "c8:1e:e7" to "Apple Inc.", "cc:08:e0" to "Apple Inc.",
        "d0:23:db" to "Apple Inc.", "d4:9a:20" to "Apple Inc.", "d8:bb:2c" to "Apple Inc.",
        "dc:0c:5c" to "Apple Inc.", "dc:56:e7" to "Apple Inc.", "e0:ac:cb" to "Apple Inc.",
        "e4:ce:8f" to "Apple Inc.", "e8:04:0b" to "Apple Inc.", "ec:85:2f" to "Apple Inc.",
        "f0:18:98" to "Apple Inc.", "f0:db:e2" to "Apple Inc.", "f4:0f:24" to "Apple Inc.",
        "f4:37:b7" to "Apple Inc.", "f8:1e:df" to "Apple Inc.", "fc:e9:98" to "Apple Inc.",

        // Samsung
        "00:02:78" to "Samsung Electronics", "00:09:18" to "Samsung Electronics",
        "00:12:47" to "Samsung Electronics", "00:13:77" to "Samsung Electronics",
        "00:15:b9" to "Samsung Electronics", "00:16:32" to "Samsung Electronics",
        "00:17:c9" to "Samsung Electronics", "00:18:af" to "Samsung Electronics",
        "00:1a:8a" to "Samsung Electronics", "00:1b:98" to "Samsung Electronics",
        "00:1c:43" to "Samsung Electronics", "00:1d:25" to "Samsung Electronics",
        "00:1e:7d" to "Samsung Electronics", "00:21:19" to "Samsung Electronics",
        "00:23:39" to "Samsung Electronics", "00:24:54" to "Samsung Electronics",
        "00:26:37" to "Samsung Electronics", "04:18:d6" to "Samsung Electronics",
        "08:08:c2" to "Samsung Electronics", "08:d4:2b" to "Samsung Electronics",
        "0c:14:20" to "Samsung Electronics", "10:1d:c0" to "Samsung Electronics",
        "14:49:e0" to "Samsung Electronics", "14:bb:6e" to "Samsung Electronics",
        "18:3a:2d" to "Samsung Electronics", "1c:af:05" to "Samsung Electronics",
        "20:6e:9c" to "Samsung Electronics", "28:27:bf" to "Samsung Electronics",
        "2c:44:01" to "Samsung Electronics", "34:23:ba" to "Samsung Electronics",
        "38:01:97" to "Samsung Electronics", "3c:62:00" to "Samsung Electronics",
        "40:0e:85" to "Samsung Electronics", "44:4e:6d" to "Samsung Electronics",
        "48:13:7e" to "Samsung Electronics", "4c:3c:16" to "Samsung Electronics",
        "50:01:bb" to "Samsung Electronics", "54:92:be" to "Samsung Electronics",
        "5c:0a:5b" to "Samsung Electronics", "60:6b:bd" to "Samsung Electronics",
        "64:b3:10" to "Samsung Electronics", "68:27:37" to "Samsung Electronics",
        "6c:83:36" to "Samsung Electronics", "70:f9:27" to "Samsung Electronics",
        "74:45:8a" to "Samsung Electronics", "78:1f:db" to "Samsung Electronics",
        "7c:0b:c6" to "Samsung Electronics", "80:18:a7" to "Samsung Electronics",
        "84:25:db" to "Samsung Electronics", "88:32:9b" to "Samsung Electronics",
        "8c:71:f8" to "Samsung Electronics", "90:18:7c" to "Samsung Electronics",
        "94:63:d1" to "Samsung Electronics", "98:52:b1" to "Samsung Electronics",
        "9c:02:98" to "Samsung Electronics", "a0:07:98" to "Samsung Electronics",
        "a4:eb:d3" to "Samsung Electronics", "a8:06:00" to "Samsung Electronics",
        "ac:5f:3e" to "Samsung Electronics", "b0:47:bf" to "Samsung Electronics",
        "b4:07:f9" to "Samsung Electronics", "bc:20:a4" to "Samsung Electronics",
        "c0:bd:d1" to "Samsung Electronics", "c4:42:02" to "Samsung Electronics",
        "c8:ba:94" to "Samsung Electronics", "cc:07:ab" to "Samsung Electronics",
        "d0:17:6a" to "Samsung Electronics", "d4:88:90" to "Samsung Electronics",
        "d8:31:cf" to "Samsung Electronics", "dc:71:96" to "Samsung Electronics",
        "e0:91:f5" to "Samsung Electronics", "e4:12:1d" to "Samsung Electronics",
        "e8:50:8b" to "Samsung Electronics", "ec:1f:72" to "Samsung Electronics",
        "f0:08:f1" to "Samsung Electronics", "f4:42:8f" to "Samsung Electronics",
        "f8:04:2e" to "Samsung Electronics", "fc:00:12" to "Samsung Electronics",

        // Google / Pixel
        "00:1a:11" to "Google LLC", "08:9e:08" to "Google LLC", "3c:5a:b4" to "Google LLC",
        "48:d6:d5" to "Google LLC", "54:60:09" to "Google LLC", "94:95:a0" to "Google LLC",
        "f4:f5:d8" to "Google LLC", "58:6e:10" to "Google LLC",

        // Xiaomi
        "00:9e:c8" to "Xiaomi", "04:cf:8c" to "Xiaomi", "0c:1d:af" to "Xiaomi",
        "10:2a:b3" to "Xiaomi", "14:f6:5a" to "Xiaomi", "20:6d:31" to "Xiaomi",
        "28:6c:07" to "Xiaomi", "34:80:b3" to "Xiaomi", "38:a4:ed" to "Xiaomi",
        "40:31:3c" to "Xiaomi", "50:8f:4c" to "Xiaomi", "58:44:98" to "Xiaomi",
        "64:09:80" to "Xiaomi", "64:b4:73" to "Xiaomi", "68:df:dd" to "Xiaomi",
        "74:23:44" to "Xiaomi", "7c:1d:d9" to "Xiaomi", "8c:be:be" to "Xiaomi",
        "9c:99:a0" to "Xiaomi", "a0:86:c6" to "Xiaomi", "ac:c1:ee" to "Xiaomi",
        "b0:e2:35" to "Xiaomi", "c4:0b:cb" to "Xiaomi", "d4:97:0b" to "Xiaomi",
        "e4:46:da" to "Xiaomi", "f0:b4:29" to "Xiaomi", "fc:64:ba" to "Xiaomi",

        // Huawei
        "00:18:82" to "Huawei", "00:1e:10" to "Huawei", "00:25:9e" to "Huawei",
        "04:02:1f" to "Huawei", "04:75:03" to "Huawei", "04:bd:70" to "Huawei",
        "04:c0:6f" to "Huawei", "08:19:a6" to "Huawei", "10:1b:54" to "Huawei",
        "14:a5:1a" to "Huawei", "18:c5:8a" to "Huawei", "20:08:ed" to "Huawei",
        "28:31:52" to "Huawei", "2c:ab:00" to "Huawei", "34:29:12" to "Huawei",
        "38:f8:89" to "Huawei", "40:4d:8e" to "Huawei", "44:6e:e5" to "Huawei",
        "48:46:fb" to "Huawei", "4c:1f:cc" to "Huawei", "50:9f:27" to "Huawei",
        "54:89:98" to "Huawei", "58:2a:f7" to "Huawei", "5c:c3:07" to "Huawei",
        "60:de:44" to "Huawei", "64:3e:8c" to "Huawei", "68:89:c1" to "Huawei",
        "6c:b1:58" to "Huawei", "70:72:3c" to "Huawei", "74:a0:28" to "Huawei",
        "78:1d:ba" to "Huawei", "7c:a7:b0" to "Huawei", "80:fb:06" to "Huawei",
        "88:a2:5e" to "Huawei", "8c:0d:76" to "Huawei", "90:17:3f" to "Huawei",
        "94:77:2b" to "Huawei", "98:e7:f5" to "Huawei", "9c:28:ef" to "Huawei",
        "a0:08:6f" to "Huawei", "a4:50:46" to "Huawei", "ac:4e:91" to "Huawei",
        "b0:e5:ed" to "Huawei", "b4:cd:27" to "Huawei", "bc:3f:8f" to "Huawei",
        "c0:70:09" to "Huawei", "c4:07:2f" to "Huawei", "c8:14:79" to "Huawei",
        "cc:96:a0" to "Huawei", "d0:7a:b5" to "Huawei", "d4:6a:a8" to "Huawei",
        "d8:c8:e9" to "Huawei", "dc:d2:fc" to "Huawei", "e0:19:54" to "Huawei",
        "e4:a4:71" to "Huawei", "e8:cd:2d" to "Huawei", "ec:cb:30" to "Huawei",
        "f4:55:9c" to "Huawei", "f8:01:13" to "Huawei", "fc:3f:db" to "Huawei",

        // OnePlus
        "08:f6:9c" to "OnePlus Technology", "18:62:e4" to "OnePlus Technology",
        "2c:0b:e9" to "OnePlus Technology", "8c:8d:28" to "OnePlus Technology",
        "ac:b6:87" to "OnePlus Technology",

        // Realme / OPPO
        "00:1b:e9" to "OPPO Electronics", "20:47:da" to "OPPO Electronics",
        "2c:f0:a2" to "OPPO Electronics", "58:a2:b5" to "OPPO Electronics",
        "88:11:96" to "OPPO Electronics", "a4:77:33" to "OPPO Electronics",
        "bc:47:60" to "OPPO Electronics", "c4:27:95" to "OPPO Electronics",
        "e8:f5:e0" to "OPPO Electronics",

        // Intel (Windows laptops)
        "00:02:b3" to "Intel Corporate", "00:03:47" to "Intel Corporate",
        "00:04:23" to "Intel Corporate", "00:07:e9" to "Intel Corporate",
        "00:0e:0c" to "Intel Corporate", "00:12:f0" to "Intel Corporate",
        "00:13:02" to "Intel Corporate", "00:13:20" to "Intel Corporate",
        "00:13:e8" to "Intel Corporate", "00:15:00" to "Intel Corporate",
        "00:16:76" to "Intel Corporate", "00:16:ea" to "Intel Corporate",
        "00:17:08" to "Intel Corporate", "00:18:de" to "Intel Corporate",
        "00:19:d2" to "Intel Corporate", "00:1b:21" to "Intel Corporate",
        "00:1c:bf" to "Intel Corporate", "00:1d:e0" to "Intel Corporate",
        "00:1e:64" to "Intel Corporate", "00:1e:65" to "Intel Corporate",
        "00:1f:3b" to "Intel Corporate", "00:21:6a" to "Intel Corporate",
        "00:22:fb" to "Intel Corporate", "00:23:14" to "Intel Corporate",
        "00:24:d6" to "Intel Corporate", "00:24:d7" to "Intel Corporate",
        "00:26:c6" to "Intel Corporate", "00:27:10" to "Intel Corporate",
        "04:0e:3c" to "Intel Corporate", "08:11:96" to "Intel Corporate",
        "10:02:b5" to "Intel Corporate", "24:77:03" to "Intel Corporate",
        "40:25:c2" to "Intel Corporate", "48:51:b7" to "Intel Corporate",
        "50:7b:9d" to "Intel Corporate", "60:36:dd" to "Intel Corporate",
        "68:05:ca" to "Intel Corporate", "80:86:f2" to "Intel Corporate",
        "8c:8d:28" to "Intel Corporate", "a4:4e:31" to "Intel Corporate",
        "b4:b6:86" to "Intel Corporate", "d0:50:99" to "Intel Corporate",
        "e8:b1:fc" to "Intel Corporate", "f4:06:69" to "Intel Corporate",

        // Realtek (Windows laptops/desktops)
        "00:01:6c" to "Realtek", "00:e0:4c" to "Realtek", "52:54:00" to "Realtek (QEMU)",

        // Google Chromecast / Nest
        "6c:ad:f8" to "Google (Chromecast)", "cc:fa:00" to "Google (Nest)",
        "d4:f5:47" to "Google (Chromecast)", "f4:f5:d8" to "Google (Chromecast)",

        // Raspberry Pi
        "28:cd:c1" to "Raspberry Pi Foundation", "b8:27:eb" to "Raspberry Pi Foundation",
        "d8:3a:dd" to "Raspberry Pi Foundation", "dc:a6:32" to "Raspberry Pi Foundation",
        "e4:5f:01" to "Raspberry Pi Foundation",

        // Amazon (Echo, Fire tablets)
        "00:bb:3a" to "Amazon Technologies", "0c:47:c9" to "Amazon Technologies",
        "34:d2:70" to "Amazon Technologies", "40:b4:cd" to "Amazon Technologies",
        "44:65:0d" to "Amazon Technologies", "50:f5:da" to "Amazon Technologies",
        "68:37:e9" to "Amazon Technologies", "74:c2:46" to "Amazon Technologies",
        "84:d6:d0" to "Amazon Technologies", "a0:02:dc" to "Amazon Technologies",
        "b4:7c:9c" to "Amazon Technologies", "f0:27:2d" to "Amazon Technologies",
        "fc:65:de" to "Amazon Technologies",
    )

    /**
     * Look up the vendor for a given MAC address.
     * MAC should be in lowercase colon-separated form: "aa:bb:cc:dd:ee:ff"
     */
    fun lookup(mac: String): String {
        if (mac.length < 8) return "Unknown"
        val oui = mac.lowercase().take(8) // "aa:bb:cc"
        return entries[oui] ?: "Unknown"
    }
}
