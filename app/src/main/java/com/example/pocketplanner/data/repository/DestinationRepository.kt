package com.example.pocketplanner.data.repository

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color

// --- 1. THE DATA CLASSES ---

data class PlaceHighlight(
    val title: String,
    val tagColor: Color,
    val imageUrl: String
)

data class Subplace(
    val name: String,
    val subtitle: String,
    val description: String,
    val countryName: String,
    val flagEmoji: String,
    val heroImageUrl: String,
    val mapImageUrl: String,
    val highlights: List<PlaceHighlight>,
    val openingTime: String,
    val ticketPrice: String,
    val theme: String // Required for your ExploreScreen filters!
)

data class City(
    val id: String,
    val title: String,
    val tagline: String,
    val description: String,
    val flagEmoji: String,
    val heroImageUrl: String,
    val theme: String, // Required for your ExploreScreen filters!
    val topPlaces: List<Subplace> // The Parent holds the Children
)


// --- 2. THE CENTRALIZED REPOSITORY ---

object DestinationRepository {

    // The Single Source of Truth
    val cities = listOf(

        // ============================
        // 1. HO CHI MINH CITY
        // ============================
        City(
            id = "Ho Chi Minh City",
            title = "Ho Chi Minh City",
            tagline = "The Pearl of the Far East, bustling & vibrant",
            description = "As the vibrant heartbeat and economic engine of Vietnam, Ho Chi Minh City is a dynamic metropolis where rich history, world-class street food, and modern energy seamlessly collide.",
            flagEmoji = "🇻🇳",
            heroImageUrl = "https://static.vinwonders.com/production/ho-chi-minh-city.jpg",
            theme = "City",
            topPlaces = listOf(
                Subplace(
                    name = "The Independence Palace",
                    subtitle = "Historic landmark of reunification",
                    description = "From its origins as the French colonial seat of Norodom Palace in 1868 to its pivotal 1954 renaming and the dramatic rise and fall of the Ngô Đình Diệm presidency, this historic landmark offers a captivating, century-long journey through the defining political shifts of modern Vietnam.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/07/41/18/98/the-independence-palace.jpg?w=1000&h=-1&s=1",
                    mapImageUrl = "https://picsum.photos/seed/hcmcmap3/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("ARCHITECTURE", Color(0xFF4496D8), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/26/64/fb/58/caption.jpg?w=1000&h=-1&s=1")
                    ),
                    openingTime = "7:00 AM - 6:00 PM",
                    ticketPrice = "80,000 VND",
                    theme = "Culture"
                ),
                Subplace(
                    name = "War Remnants Museum",
                    subtitle = "Poignant exhibits on the Vietnam War",
                    description = "A sobering and powerful museum documenting the brutal realities of the Vietnam War. Features extensive photo exhibits, military equipment, and historical artifacts.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/06/9e/ce/a3/war-remnants-museum.jpg?w=1000&h=-1&s=1",
                    mapImageUrl = "https://picsum.photos/seed/hcmcmap2/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("HISTORY", Color(0xFF4496D8), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/33/ef/fb/16/caption.jpg?w=1000&h=-1&s=1"),
                        PlaceHighlight("MILITARY EQUIPMENT", Color(0xFF4496D8), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/33/e7/60/0d/caption.jpg?w=1000&h=-1&s=1")
                    ),
                    openingTime = "7:30 AM - 5:30 PM",
                    ticketPrice = "40,000 VND",
                    theme = "Culture"
                ),
                Subplace(
                    name = "Cu Chi Tunnels",
                    subtitle = "Vietnam’s legendary subterranean fortress of guerrilla ingenuity",
                    description = "A historic underground network of wartime strategy, hands-on adventure, and thrilling firsthand exploration.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/1b/2a/05/1c/photo8jpg.jpg?w=1000&h=-1&s=1",
                    mapImageUrl = "https://picsum.photos/seed/hcmcmap/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("MILITARY EQUIPMENT", Color(0xFF4496D8), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/1a/3f/82/4c/img-20191209-113051-largejpg.jpg?w=1000&h=-1&s=1"),
                        PlaceHighlight("HISTORY", Color(0xFF4496D8), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0a/78/2b/b2/inside-of-tunnel.jpg?w=1000&h=-1&s=1")
                    ),
                    openingTime = "7:00 AM – 5:00 PM",
                    ticketPrice = "90,000 - 110,000 VND",
                    theme = "Culture"
                ),
                Subplace(
                    name = "Saigon Zoo & Botanical Gardens",
                    subtitle = "The world’s eighth-oldest zoo, blending centuries of botanical heritage with timeless city memories",
                    description = "From its storied founding in 1864 as the world’s eighth-oldest zoo and lush botanical grounds to hands-on ecological education and cherished memories spanning generations, Saigon Zoo & Botanical Gardens offers a serene, verdant historical journey through the heart of Ho Chi Minh City.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://saigonzoo.vn/uploads/anh-dep-tcv/truong-dinh-7.jpg",
                    mapImageUrl = "https://picsum.photos/seed/hcmcmap/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("ANIMAL", Color(0xFF4496D8), "https://saigonzoo.vn/uploads/anh-dep-tcv/hai-vo-3.jpg")
                    ),
                    openingTime = "7:00 AM – 6:30 PM",
                    ticketPrice = "60,000 VND",
                    theme = "Nature"
                ),
                Subplace(
                    name = "Banh Mi Huynh Hoa",
                    subtitle = "Saigon’s most legendary bánh mì, packed with world-renowned layers of savory indulgence",
                    description = "From its storied 35-year street-food legacy and glowing international acclaim to generous mounds of savory meats and rich, velvety pâté that capture the essence of a culinary paradise, Bánh Mì Huỳnh Hoa offers an unforgettable, flavor-packed gastronomic journey through Saigon.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://img.vietcetera.com/uploads/images/31-dec-2024/dscf0592.jpg",
                    mapImageUrl = "https://picsum.photos/seed/hcmcmap/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("FOOD", Color(0xFF4496D8), "https://cdn2.tuoitre.vn/tcdulichtphcm/media/upload/4-2021/images/2021-12-18/picture-1-1639846674-180-width750height779.jpg")
                    ),
                    openingTime = "6:00 AM – 10:00 PM",
                    ticketPrice = "28,000 - 78,000 VND",
                    theme = "Food"
                )
            )
        ),

        // ============================
        // 2. HANOI
        // ============================
        City(
            id = "Hanoi",
            title = "Hanoi",
            tagline = "The thousand-year-old soul of Vietnam, where tranquil lakes meet the timeless charm of ancient alleys",
            description = "From ancient pagodas shrouded in incense and tranquil waterside temples to cozy hidden coffee shops and vibrant artisan guilds, Hanoi offers a poetic cultural escape through the historic heart of Vietnam.",
            flagEmoji = "🇻🇳",
            heroImageUrl = "https://www.deshvideshtravels.com/_next/image?url=https%3A%2F%2Fdeshvideshstrapi.s3.ap-south-1.amazonaws.com%2FHanoi_Travel_Guide_518489b1ec.jpg&w=1920&q=75",
            theme = "City",
            topPlaces = listOf(
                Subplace(
                    name = "Hoa Lo Prison",
                    subtitle = "Hanoi’s infamous wartime prison, now preserved as a poignant museum of survival and resilience.",
                    description = "From its chilling past as the notorious 'Hanoi Hilton' and sombre colonial-era cellblocks to its preserved historic gatehouse and evocative museum displays, Hỏa Lò Prison offers a sobering, deeply moving journey into the enduring resilience of the human spirit.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/1b/2c/0d/94/photo7jpg.jpg?w=1000&h=-1&s=1",
                    mapImageUrl = "https://picsum.photos/seed/hanoimap/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("HISTORY", Color(0xFFF9184B), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/34/33/49/92/caption.jpg?w=1000&h=-1&s=1")
                    ),
                    openingTime = "8:00 AM - 5:00 PM",
                    ticketPrice = "50,000 VND",
                    theme = "Culture"
                ),
                Subplace(
                    name = "Ho Chi Minh Mausoleum",
                    subtitle = "Vietnam’s most sacred national monument, honoring the enduring legacy of President Ho Chi Minh",
                    description = "From its imposing gray granite colonnades and crimson jade inscriptions to the solemn changing of the guard and peaceful gardens featuring over 250 symbolic plant species, the Ho Chi Minh Mausoleum offers a deeply reverent, iconic journey into the heart of Vietnam’s national heritage.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/1b/32/93/64/mausoleum.jpg?w=1000&h=-1&s=1",
                    mapImageUrl = "https://picsum.photos/seed/hanoimap2/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("HISTORY", Color(0xFF33574A), "https://statics.vinpearl.com/ho-chi-minh-mausoleum-3_1662727235.jpg")
                    ),
                    openingTime = "7:30 AM - 10:30 AM (From April 1 - October 31) / 8:00 AM - 11:00 AM (From November 1 - March 31). Every Tuesday, Wednesday, Thursday, Saturday, and Sunday)",
                    ticketPrice = "25,000 VND",
                    theme = "Culture"
                ),
                Subplace(
                    name = "St. Joseph's Cathedral",
                    subtitle = "Hanoi’s oldest cathedral, standing as a resilient architectural masterpiece through centuries of change",
                    description = "From its historic origins as one of the earliest French colonial landmarks in Indochina and its miraculous survival through two fierce wars to its striking weathered facade and bustling plaza beloved by locals and travelers alike, St. Joseph’s Cathedral offers a timeless, evocative journey through Hanoi’s enduring heritage.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0c/32/bb/34/nha-th-l-n-ha-n-i-40.jpg?w=1000&h=-1&s=1",
                    mapImageUrl = "https://picsum.photos/seed/hanoimap2/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("ARCHITECTURE", Color(0xFF33574A), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/32/d8/fb/01/caption.jpg?w=1000&h=-1&s=1")
                    ),
                    openingTime = "5:00 AM - 11:30 AM / 2:00 PM - 7:30 PM",
                    ticketPrice = "Free",
                    theme = "Culture"
                ),
                Subplace(
                    name = "Hoan Kiem Walking Street",
                    subtitle = "Hanoi’s vibrant weekend pedestrian haven, alive with street music, joyful crowds, and delicious flavors",
                    description = "From the lively buzz of car-free lakeside promenades and dynamic live music performances to endless entertainment options and mouthwatering local street food, Hoan Kiem Lake Walking Street offers an electrifying, joyful journey through Hanoi’s weekend energy.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/17/ed/88/67/lake-at-night.jpg?w=1000&h=-1&s=1",
                    mapImageUrl = "https://picsum.photos/seed/hanoimap2/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("NATURE", Color(0xFF33574A), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/17/ed/86/3a/red-bridge.jpg?w=1000&h=-1&s=1")
                    ),
                    openingTime = "7:00 PM on Friday - Sunday midnight.",
                    ticketPrice = "Free",
                    theme = "Nature"
                ),
                Subplace(
                    name = "Bun Cha Huong Lien",
                    subtitle = "Hanoi’s iconic presidential noodle spot, legendary for its smoky grilled pork and famous 'Obama Combo'.",
                    description = "From the famous glass-encased 'president table' and walls of celebrity memorabilia to sizzling smoky pork patties, crispy fried seafood rolls, and ice-cold Bia Hà Nội in the signature 'Obama Combo,' this legendary eatery offers an unforgettable, flavor-packed culinary journey through Hanoi’s most celebrated comfort food.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/16/94/e6/99/caption.jpg?w=1000&h=-1&s=1",
                    mapImageUrl = "https://picsum.photos/seed/hanoimap2/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("FOOD", Color(0xFF33574A), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/26/cd/d4/95/bun-cha.jpg?w=1000&h=-1&s=1")
                    ),
                    openingTime = "8:30 AM - 2:30 PM / 5:00 PM - 8:30 PM",
                    ticketPrice = "130,000 VND",
                    theme = "Food"
                )
            )
        ),

        // ============================
        // 3. DA NANG
        // ============================
        City(
            id = "Da Nang",
            title = "Da Nang",
            tagline = "Coastal beauty and modern bridges",
            description = "Known for its sandy beaches, the stunning Marble Mountains, and the iconic Golden Bridge held by giant stone hands, Da Nang is Central Vietnam's most vibrant coastal city.",
            flagEmoji = "🇻🇳",
            heroImageUrl = "https://i0.wp.com/littlebirdietravel.com/wp-content/uploads/2025/07/hand-bridge-da-nang.jpg?fit=2048%2C1152&ssl=1",
            theme = "Beach",
            topPlaces = listOf(
                Subplace(
                    name = "Ba Na Hills SunWorld",
                    subtitle = "A fantastical mountaintop resort",
                    description = "Accessible via one of the world's longest cable cars, this resort features the famous Golden Bridge, a French Village, and panoramic views of the coastal region.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/31/af/74/d8/sun-world-ba-na-hills.jpg?w=1000&h=-1&s=1",
                    mapImageUrl = "https://picsum.photos/seed/danangmap/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("SCENERY", Color(0xFF26A8F0), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/31/af/96/46/discover-sun-world-ba.jpg?w=1000&h=-1&s=1")
                    ),
                    openingTime = "8:00 AM - 10:00 PM",
                    ticketPrice = "650,000 - 1,050,000 VND (Includes cable car)",
                    theme = "Nature"
                ),
                Subplace(
                    name = "Son Tra Beach",
                    subtitle = "Central Vietnam's pristine coastline",
                    description = "Famous for its blue sky, smooth white sand, and gentle slope. It's an ideal spot for swimming, surfing, and relaxing by the ocean.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/2d/d9/31/54/son-tra-beach.jpg?w=1000&h=-1&s=1",
                    mapImageUrl = "https://picsum.photos/seed/danangmap2/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("RELAX", Color(0xFFF0904E), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/2d/d9/31/53/son-tra-beach.jpg?w=1000&h=-1&s=1")
                    ),
                    openingTime = "Open 24/7",
                    ticketPrice = "Free Entry",
                    theme = "Beach"
                ),
                Subplace(
                    name = "Golden Bridge",
                    subtitle = "Vietnam’s world-famous skybridge, cradled high in the mountain clouds by colossal giant hands",
                    description = "From its gleaming 150-meter curved golden walkway and the awe-inspiring spectacle of two colossal weathered hands rising from the cliffside to sweeping mountaintop vistas and seamless paths through lush gardens, the Golden Bridge offers a surreal, unforgettable aerial journey high above Da Nang.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/16/45/31/f5/golden-bridge-under-sunny.jpg?w=1000&h=-1&s=1",
                    mapImageUrl = "https://picsum.photos/seed/danangmap2/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("SCENERY", Color(0xFFF0904E), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/34/33/06/03/caption.jpg?w=1000&h=-1&s=1")
                    ),
                    openingTime = "8:00 AM - 7:00 PM",
                    ticketPrice = "Access is included with your general admission ticket to Sun World Ba Na Hills",
                    theme = "Nature"
                )
            )
        ),

        // ============================
        // 4. HUE
        // ============================
        City(
            id = "Hue",
            title = "Hue",
            tagline = "The ancient imperial capital",
            description = "Sitting on the banks of the Perfume River, Hue was the seat of Nguyen Dynasty emperors. It is celebrated for its majestic Imperial City, royal tombs, and refined cuisine.",
            flagEmoji = "🇻🇳",
            heroImageUrl = "https://media.vietravel.com/images/Content/dia-diem-du-lich-hue-1169.jpg",
            theme = "Culture",
            topPlaces = listOf(
                Subplace(
                    name = "Hue Imperial City (The Citadel)",
                    subtitle = "The walled palace of the Nguyen Dynasty",
                    description = "A vast 19th-century complex modeled after Beijing's Forbidden City, featuring moats, imposing gates, palaces, and intricately decorated pavilions.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/11/7a/57/7b/vue-interieure.jpg?w=1000&h=-1&s=1",
                    mapImageUrl = "https://picsum.photos/seed/huemap/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("HISTORY", Color(0xFFBE7898), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/33/d6/78/70/caption.jpg?w=800&h=-1&s=1")
                    ),
                    openingTime = "7:00 AM - 5:30 PM",
                    ticketPrice = "150,000 VND",
                    theme = "Culture"
                ),
                Subplace(
                    name = "Thien Mu Pagoda",
                    subtitle = "Hue’s most iconic spiritual symbol, crowned by an elegant seven-tiered tower and peaceful monastic grounds",
                    description = "From its striking seven-tiered monument standing proudly at the entrance and the resonant echoes of its historic bell tower to peaceful working halls and tranquil living quarters for resident monks, Thiên Mụ Pagoda offers a deeply spiritual, poetic journey through Hue’s most cherished sacred landmark.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0c/8c/03/ac/another-view-of-the-pagoda.jpg?w=1000&h=-1&s=1",
                    mapImageUrl = "https://picsum.photos/seed/huemap/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("ARCHITECTURE", Color(0xFFBE7898), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0c/8a/5d/5c/photo0jpg.jpg?w=1000&h=-1&s=1")
                    ),
                    openingTime = "8:00 AM - 6:00 PM",
                    ticketPrice = "Free",
                    theme = "Culture"
                ),
                Subplace(
                    name = "Lang Co Beach",
                    subtitle = "A royal 'fairyland on earth', where ten kilometers of pristine white sands meet calm turquoise seas",
                    description = "From its sweeping ten-kilometer stretch of soft white sands and tranquil azure waters to the dramatic backdrop of the Bạch Mã peaks and its storied legacy as Emperor Khải Định’s royal paradise, Lăng Cô Beach offers an enchanting, serene coastal escape along Central Vietnam’s most breathtaking shoreline.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/19/33/e6/68/lang-co-beach.jpg?w=1000&h=-1&s=1",
                    mapImageUrl = "https://picsum.photos/seed/huemap/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("RELAX", Color(0xFFBE7898), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/0d/9d/cf/9b/lang-co.jpg?w=1000&h=-1&s=1")
                    ),
                    openingTime = "Open 24/7",
                    ticketPrice = "Free",
                    theme = "Beach"
                ),
                Subplace(
                    name = "Bach Ma National Park",
                    subtitle = "A pristine natural sanctuary nestled between mountains and sea, where cool cloud forests and rare wildlife thrive",
                    description = "From the towering peaks of the Annamite Mountains and sweeping coastal views toward the East Sea to cool, misty cloud forests and elusive rare wildlife, Bạch Mã National Park offers an invigorating ecological escape into a hidden, deeply alive mountain paradise.",
                    countryName = "Vietnam",
                    flagEmoji = "🇻🇳",
                    heroImageUrl = "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/05/36/f4/c0/bach-ma-national-park.jpg?w=1000&h=-1&s=1",
                    mapImageUrl = "https://picsum.photos/seed/huemap/1000/1200",
                    highlights = listOf(
                        PlaceHighlight("HIKING", Color(0xFFBE7898), "https://dynamic-media-cdn.tripadvisor.com/media/photo-o/12/89/40/df/cars-and-vans-are-allowed.jpg?w=1000&h=-1&s=1")
                    ),
                    openingTime = "7:30 AM - 5:00 PM",
                    ticketPrice = "65,000 VND",
                    theme = "Nature"
                )
            )
        )
    )

    // --- HELPER FUNCTION FOR EXPLORE SCREEN ---
    // This magically flattens every Subplace from every City into one giant list!
    // It allows your "Under the radar", "Search", and "Saved" sections to easily grab the individual places.
    val allSubplaces: List<Subplace>
        get() = cities.flatMap { it.topPlaces }
}