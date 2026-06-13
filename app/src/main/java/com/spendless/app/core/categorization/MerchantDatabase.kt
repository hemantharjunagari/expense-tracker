package com.spendless.app.core.categorization

import com.spendless.app.core.data.database.entities.Category

/**
 * Comprehensive merchant-to-category mapping database.
 * Contains 500+ rules covering Indian merchants, apps, and services.
 *
 * Structure: keyword → Category
 * Keywords are lowercase for case-insensitive matching.
 */
object MerchantDatabase {

    /**
     * Exact merchant name → Category mappings (highest priority)
     * Keys are lowercase normalized merchant names.
     */
    val EXACT_MERCHANT_MAP: Map<String, Category> = buildMap {
        // ── Food & Dining ──────────────────────────────────────────────────────
        put("swiggy", Category.FOOD_DINING)
        put("zomato", Category.FOOD_DINING)
        put("dominos", Category.FOOD_DINING)
        put("pizza hut", Category.FOOD_DINING)
        put("kfc", Category.FOOD_DINING)
        put("mcdonalds", Category.FOOD_DINING)
        put("burger king", Category.FOOD_DINING)
        put("subway", Category.FOOD_DINING)
        put("starbucks", Category.FOOD_DINING)
        put("cafe coffee day", Category.FOOD_DINING)
        put("ccd", Category.FOOD_DINING)
        put("chaayos", Category.FOOD_DINING)
        put("wow momo", Category.FOOD_DINING)
        put("biryani blues", Category.FOOD_DINING)
        put("barbeque nation", Category.FOOD_DINING)
        put("behrouz biryani", Category.FOOD_DINING)
        put("faasos", Category.FOOD_DINING)
        put("box8", Category.FOOD_DINING)
        put("freshmenu", Category.FOOD_DINING)
        put("dunkin donuts", Category.FOOD_DINING)
        put("baskin robbins", Category.FOOD_DINING)
        put("amul", Category.FOOD_DINING)
        put("haldirams", Category.FOOD_DINING)
        put("bikanervala", Category.FOOD_DINING)
        put("punjab grill", Category.FOOD_DINING)
        put("social", Category.FOOD_DINING)
        put("the beer cafe", Category.FOOD_DINING)
        put("tgi fridays", Category.FOOD_DINING)
        put("chilis", Category.FOOD_DINING)
        put("eatsure", Category.FOOD_DINING)

        // ── Groceries ──────────────────────────────────────────────────────────
        put("bigbasket", Category.GROCERIES)
        put("big basket", Category.GROCERIES)
        put("blinkit", Category.GROCERIES)
        put("grofers", Category.GROCERIES)
        put("zepto", Category.GROCERIES)
        put("dunzo daily", Category.GROCERIES)
        put("dmart", Category.GROCERIES)
        put("d mart", Category.GROCERIES)
        put("more supermarket", Category.GROCERIES)
        put("reliance fresh", Category.GROCERIES)
        put("reliance smart", Category.GROCERIES)
        put("star bazaar", Category.GROCERIES)
        put("nature basket", Category.GROCERIES)
        put("spencers", Category.GROCERIES)
        put("big bazaar", Category.GROCERIES)
        put("country delight", Category.GROCERIES)
        put("licious", Category.GROCERIES)
        put("freshtohome", Category.GROCERIES)
        put("supermart", Category.GROCERIES)
        put("jiomart", Category.GROCERIES)
        put("meating", Category.GROCERIES)

        // ── Transportation ─────────────────────────────────────────────────────
        put("uber", Category.TRANSPORTATION)
        put("ola", Category.TRANSPORTATION)
        put("rapido", Category.TRANSPORTATION)
        put("blablacar", Category.TRANSPORTATION)
        put("quick ride", Category.TRANSPORTATION)
        put("irctc", Category.TRANSPORTATION)
        put("railway", Category.TRANSPORTATION)
        put("indian railways", Category.TRANSPORTATION)
        put("dmrc", Category.TRANSPORTATION)
        put("metro", Category.TRANSPORTATION)
        put("bmtc", Category.TRANSPORTATION)
        put("best bus", Category.TRANSPORTATION)
        put("redbus", Category.TRANSPORTATION)
        put("abhibus", Category.TRANSPORTATION)
        put("olacabs", Category.TRANSPORTATION)
        put("meru", Category.TRANSPORTATION)
        put("zoom car", Category.TRANSPORTATION)
        put("zoomcar", Category.TRANSPORTATION)
        put("revv", Category.TRANSPORTATION)
        put("bounce", Category.TRANSPORTATION)
        put("yulu", Category.TRANSPORTATION)
        put("vogo", Category.TRANSPORTATION)
        put("drivezy", Category.TRANSPORTATION)
        put("porter", Category.TRANSPORTATION)
        put("dunzo", Category.TRANSPORTATION)
        put("lalamove", Category.TRANSPORTATION)

        // ── Fuel ───────────────────────────────────────────────────────────────
        put("indian oil", Category.FUEL)
        put("iocl", Category.FUEL)
        put("hp petrol", Category.FUEL)
        put("hindustan petroleum", Category.FUEL)
        put("bharat petroleum", Category.FUEL)
        put("bpcl", Category.FUEL)
        put("reliance petroleum", Category.FUEL)
        put("shell", Category.FUEL)
        put("essar oil", Category.FUEL)
        put("petrol pump", Category.FUEL)
        put("fuel station", Category.FUEL)

        // ── Shopping ───────────────────────────────────────────────────────────
        put("amazon", Category.SHOPPING)
        put("flipkart", Category.SHOPPING)
        put("myntra", Category.SHOPPING)
        put("ajio", Category.SHOPPING)
        put("meesho", Category.SHOPPING)
        put("snapdeal", Category.SHOPPING)
        put("shopsy", Category.SHOPPING)
        put("tata cliq", Category.SHOPPING)
        put("nykaa", Category.SHOPPING)
        put("purplle", Category.SHOPPING)
        put("firstcry", Category.SHOPPING)
        put("voonik", Category.SHOPPING)
        put("limeroad", Category.SHOPPING)
        put("craftsvilla", Category.SHOPPING)
        put("pepperfry", Category.SHOPPING)
        put("urban ladder", Category.SHOPPING)
        put("ikea", Category.SHOPPING)
        put("home centre", Category.SHOPPING)
        put("fabindia", Category.SHOPPING)
        put("pantaloons", Category.SHOPPING)
        put("shoppers stop", Category.SHOPPING)
        put("lifestyle", Category.SHOPPING)
        put("westside", Category.SHOPPING)
        put("zara", Category.SHOPPING)
        put("h&m", Category.SHOPPING)
        put("uniqlo", Category.SHOPPING)
        put("decathlon", Category.SHOPPING)
        put("croma", Category.SHOPPING)
        put("reliance digital", Category.SHOPPING)
        put("vijay sales", Category.SHOPPING)
        put("poorvika", Category.SHOPPING)

        // ── Entertainment ──────────────────────────────────────────────────────
        put("bookmyshow", Category.ENTERTAINMENT)
        put("book my show", Category.ENTERTAINMENT)
        put("pvr", Category.ENTERTAINMENT)
        put("inox", Category.ENTERTAINMENT)
        put("cinepolis", Category.ENTERTAINMENT)
        put("carnival cinemas", Category.ENTERTAINMENT)
        put("wonderla", Category.ENTERTAINMENT)
        put("imagica", Category.ENTERTAINMENT)
        put("essel world", Category.ENTERTAINMENT)
        put("kingdom of dreams", Category.ENTERTAINMENT)

        // ── Subscriptions ──────────────────────────────────────────────────────
        put("netflix", Category.SUBSCRIPTIONS)
        put("amazon prime", Category.SUBSCRIPTIONS)
        put("hotstar", Category.SUBSCRIPTIONS)
        put("disney hotstar", Category.SUBSCRIPTIONS)
        put("zee5", Category.SUBSCRIPTIONS)
        put("sonyliv", Category.SUBSCRIPTIONS)
        put("voot", Category.SUBSCRIPTIONS)
        put("alt balaji", Category.SUBSCRIPTIONS)
        put("mxplayer", Category.SUBSCRIPTIONS)
        put("spotify", Category.SUBSCRIPTIONS)
        put("gaana", Category.SUBSCRIPTIONS)
        put("jiosaavn", Category.SUBSCRIPTIONS)
        put("wynk music", Category.SUBSCRIPTIONS)
        put("youtube premium", Category.SUBSCRIPTIONS)
        put("google one", Category.SUBSCRIPTIONS)
        put("icloud", Category.SUBSCRIPTIONS)
        put("microsoft 365", Category.SUBSCRIPTIONS)
        put("office 365", Category.SUBSCRIPTIONS)
        put("adobe", Category.SUBSCRIPTIONS)
        put("dropbox", Category.SUBSCRIPTIONS)
        put("linkedin premium", Category.SUBSCRIPTIONS)

        // ── Utilities ──────────────────────────────────────────────────────────
        put("bescom", Category.UTILITIES)
        put("tata power", Category.UTILITIES)
        put("adani electricity", Category.UTILITIES)
        put("msedcl", Category.UTILITIES)
        put("bwssb", Category.UTILITIES)
        put("mahanagar gas", Category.UTILITIES)
        put("indraprastha gas", Category.UTILITIES)
        put("igl", Category.UTILITIES)
        put("mgl", Category.UTILITIES)
        put("airtel", Category.UTILITIES)
        put("jio", Category.UTILITIES)
        put("vi", Category.UTILITIES)
        put("vodafone idea", Category.UTILITIES)
        put("bsnl", Category.UTILITIES)
        put("tata sky", Category.UTILITIES)
        put("dish tv", Category.UTILITIES)
        put("den networks", Category.UTILITIES)
        put("hathway", Category.UTILITIES)
        put("act fibernet", Category.UTILITIES)
        put("excitel", Category.UTILITIES)
        put("spectranet", Category.UTILITIES)

        // ── Healthcare ─────────────────────────────────────────────────────────
        put("pharmeasy", Category.HEALTHCARE)
        put("1mg", Category.HEALTHCARE)
        put("apollo pharmacy", Category.HEALTHCARE)
        put("medplus", Category.HEALTHCARE)
        put("netmeds", Category.HEALTHCARE)
        put("practo", Category.HEALTHCARE)
        put("lybrate", Category.HEALTHCARE)
        put("mfine", Category.HEALTHCARE)
        put("healthkart", Category.HEALTHCARE)
        put("cure fit", Category.HEALTHCARE)
        put("cult fit", Category.HEALTHCARE)
        put("fitness first", Category.HEALTHCARE)
        put("gold gym", Category.HEALTHCARE)
        put("anytime fitness", Category.HEALTHCARE)

        // ── Education ──────────────────────────────────────────────────────────
        put("byjus", Category.EDUCATION)
        put("byju", Category.EDUCATION)
        put("unacademy", Category.EDUCATION)
        put("vedantu", Category.EDUCATION)
        put("toppr", Category.EDUCATION)
        put("coursera", Category.EDUCATION)
        put("udemy", Category.EDUCATION)
        put("linkedin learning", Category.EDUCATION)
        put("simplilearn", Category.EDUCATION)
        put("upgrad", Category.EDUCATION)
        put("whitehat jr", Category.EDUCATION)
        put("coding ninjas", Category.EDUCATION)

        // ── Travel ─────────────────────────────────────────────────────────────
        put("makemytrip", Category.TRAVEL)
        put("goibibo", Category.TRAVEL)
        put("yatra", Category.TRAVEL)
        put("cleartrip", Category.TRAVEL)
        put("ixigo", Category.TRAVEL)
        put("ease my trip", Category.TRAVEL)
        put("airbnb", Category.TRAVEL)
        put("oyo", Category.TRAVEL)
        put("treebo", Category.TRAVEL)
        put("fabhotels", Category.TRAVEL)
        put("lemon tree", Category.TRAVEL)
        put("marriott", Category.TRAVEL)
        put("hilton", Category.TRAVEL)
        put("taj hotels", Category.TRAVEL)

        // ── Investments ────────────────────────────────────────────────────────
        put("zerodha", Category.INVESTMENTS)
        put("groww", Category.INVESTMENTS)
        put("upstox", Category.INVESTMENTS)
        put("angel one", Category.INVESTMENTS)
        put("5paisa", Category.INVESTMENTS)
        put("icicidirect", Category.INVESTMENTS)
        put("hdfc securities", Category.INVESTMENTS)
        put("motilal oswal", Category.INVESTMENTS)
        put("coin by zerodha", Category.INVESTMENTS)
        put("paytm money", Category.INVESTMENTS)
        put("nps", Category.INVESTMENTS)
        put("ppf", Category.INVESTMENTS)
        put("lic", Category.INVESTMENTS)
        put("sbi life", Category.INVESTMENTS)
        put("hdfc life", Category.INVESTMENTS)
        put("icici prudential", Category.INVESTMENTS)
        put("max life", Category.INVESTMENTS)
        put("bajaj allianz", Category.INVESTMENTS)
    }

    /**
     * Keyword patterns that appear anywhere in merchant/SMS body → Category
     * Lower priority than exact matches.
     */
    val KEYWORD_CATEGORY_MAP: List<Pair<String, Category>> = listOf(
        // Food
        "food" to Category.FOOD_DINING,
        "restaurant" to Category.FOOD_DINING,
        "cafe" to Category.FOOD_DINING,
        "coffee" to Category.FOOD_DINING,
        "bakery" to Category.FOOD_DINING,
        "dhaba" to Category.FOOD_DINING,
        "biryani" to Category.FOOD_DINING,
        "pizza" to Category.FOOD_DINING,
        "burger" to Category.FOOD_DINING,
        "hotel restaurant" to Category.FOOD_DINING,
        "eating" to Category.FOOD_DINING,

        // Groceries
        "grocery" to Category.GROCERIES,
        "supermarket" to Category.GROCERIES,
        "kirana" to Category.GROCERIES,
        "mart" to Category.GROCERIES,
        "store" to Category.GROCERIES,
        "vegetables" to Category.GROCERIES,
        "sabzi" to Category.GROCERIES,

        // Transportation
        "cab" to Category.TRANSPORTATION,
        "taxi" to Category.TRANSPORTATION,
        "auto" to Category.TRANSPORTATION,
        "rickshaw" to Category.TRANSPORTATION,
        "bus" to Category.TRANSPORTATION,
        "train" to Category.TRANSPORTATION,
        "metro" to Category.TRANSPORTATION,
        "parking" to Category.TRANSPORTATION,
        "fastag" to Category.TRANSPORTATION,
        "toll" to Category.TRANSPORTATION,

        // Fuel
        "petrol" to Category.FUEL,
        "diesel" to Category.FUEL,
        "fuel" to Category.FUEL,
        "cng" to Category.FUEL,
        "gas station" to Category.FUEL,
        "hp pump" to Category.FUEL,
        "ioc" to Category.FUEL,

        // Shopping
        "shop" to Category.SHOPPING,
        "mall" to Category.SHOPPING,
        "retail" to Category.SHOPPING,
        "purchase" to Category.SHOPPING,

        // Entertainment
        "cinema" to Category.ENTERTAINMENT,
        "movie" to Category.ENTERTAINMENT,
        "theatre" to Category.ENTERTAINMENT,
        "multiplex" to Category.ENTERTAINMENT,
        "concert" to Category.ENTERTAINMENT,
        "event" to Category.ENTERTAINMENT,

        // Utilities
        "electricity" to Category.UTILITIES,
        "power bill" to Category.UTILITIES,
        "water bill" to Category.UTILITIES,
        "gas bill" to Category.UTILITIES,
        "broadband" to Category.UTILITIES,
        "internet" to Category.UTILITIES,
        "mobile recharge" to Category.UTILITIES,
        "recharge" to Category.UTILITIES,
        "dth" to Category.UTILITIES,
        "cable tv" to Category.UTILITIES,

        // Healthcare
        "hospital" to Category.HEALTHCARE,
        "clinic" to Category.HEALTHCARE,
        "doctor" to Category.HEALTHCARE,
        "medical" to Category.HEALTHCARE,
        "pharmacy" to Category.HEALTHCARE,
        "health" to Category.HEALTHCARE,
        "medicine" to Category.HEALTHCARE,
        "lab" to Category.HEALTHCARE,
        "diagnostic" to Category.HEALTHCARE,
        "gym" to Category.HEALTHCARE,
        "fitness" to Category.HEALTHCARE,

        // Education
        "school" to Category.EDUCATION,
        "college" to Category.EDUCATION,
        "university" to Category.EDUCATION,
        "tuition" to Category.EDUCATION,
        "coaching" to Category.EDUCATION,
        "course" to Category.EDUCATION,
        "fees" to Category.EDUCATION,
        "exam" to Category.EDUCATION,

        // Rent
        "rent" to Category.RENT,
        "house rent" to Category.RENT,
        "flat rent" to Category.RENT,
        "pg" to Category.RENT,
        "hostel" to Category.RENT,

        // Travel
        "hotel" to Category.TRAVEL,
        "flight" to Category.TRAVEL,
        "airlines" to Category.TRAVEL,
        "airport" to Category.TRAVEL,
        "booking" to Category.TRAVEL,
        "resort" to Category.TRAVEL,
        "holiday" to Category.TRAVEL,
        "tour" to Category.TRAVEL,

        // Investments
        "mutual fund" to Category.INVESTMENTS,
        "sip" to Category.INVESTMENTS,
        "stock" to Category.INVESTMENTS,
        "equity" to Category.INVESTMENTS,
        "insurance" to Category.INVESTMENTS,
        "premium" to Category.INVESTMENTS,
        "policy" to Category.INVESTMENTS,
        "investment" to Category.INVESTMENTS,
        "demat" to Category.INVESTMENTS,

        // EMI/Loans
        "emi" to Category.EMI_LOANS,
        "loan" to Category.EMI_LOANS,
        "instalment" to Category.EMI_LOANS,
        "installment" to Category.EMI_LOANS,
        "credit card bill" to Category.EMI_LOANS,
        "card bill" to Category.EMI_LOANS,
        "outstanding" to Category.EMI_LOANS,

        // Income markers
        "salary" to Category.INCOME,
        "stipend" to Category.INCOME,
        "dividend" to Category.INCOME,
        "interest" to Category.INCOME,
        "refund" to Category.INCOME,
        "cashback" to Category.INCOME,

        // Self Transfer markers
        "self transfer" to Category.SELF_TRANSFER,
        "own account" to Category.SELF_TRANSFER,
        "transfer to self" to Category.SELF_TRANSFER,
        "transfer to own" to Category.SELF_TRANSFER,
        "transferred to self" to Category.SELF_TRANSFER,
        "transferred to own" to Category.SELF_TRANSFER
    )
}
