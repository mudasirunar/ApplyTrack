package com.example.ui.dashboard

import kotlin.random.Random

object MotivationalMessages {

    private val zeroAppsMessages = listOf(
        "No applications yet? Let's take the first step today! 🚀",
        "The screen is clean, but your dreams are big. Let's add one! ✨",
        "Blank canvas! Tap the '+' button to write your success story! ✍️",
        "Zero applications? A perfect time to start fresh! 🌟",
        "Every giant leap starts with a single tap. Let's begin! 💼",
        "Ready to land that dream job? Time to make the first move! ♟️",
        "Your future employer is waiting. Let's create an entry! 🏢",
        "The hardest part is starting. You can do this! 🌅",
        "Don't wait for opportunity, create it! Press '+'! 🛠️",
        "A journey of a thousand miles begins with a single click. 🗺️",
        "Fresh start, fresh opportunities. Let's get to work! ⚡",
        "Your career adventure starts right here, right now! 🧭",
        "Time to turn plans into action! Tap to add a job. 🚀",
        "Your future self will thank you for starting today! ⏳",
        "Let's get this bread! Add your first application. 🍞",
        "The search is on. Let's build your target list! 🎯",
        "Ready, set, search! Tap the plus icon. 🏁",
        "Opportunity knocks, but you have to build the door! 🚪",
        "Believe you can and you're halfway there. Let's begin! 💫",
        "Let's make some waves! Create your first entry. 🌊"
    )

    private val smallAppsMessages = listOf(
        "First steps taken! You are officially in the game! 🌱",
        "Seed planted! Now let's water it with consistency! 💧",
        "Nice work starting out. Keep that momentum! 🚂",
        "Small steps lead to grand destinations. Keep moving! 🚶",
        "You've got the ball rolling! What's the next target? 🎯",
        "One is better than none. Keep adding to the list! 📋",
        "The journey has begun. Keep pushing ahead! 🚀",
        "Progress is progress, no matter how small. Good job! 🌟",
        "You've broken the ice. Now let's keep going! ❄️",
        "Great start! Every application is a seed of hope. 🌻",
        "Momentum is building. What's the next application? 🏎️",
        "The first few are the hardest. You're past that! 👍",
        "Step by step, brick by brick. Keep building! 🧱",
        "One, two, three! You are on your way! 🔢",
        "The search engine is running. Let's add fuel! ⛽",
        "Good beginnings make good endings. Keep it up! 🌅",
        "You've officially started. No looking back! 🗺️",
        "Every giant oak tree started as a little acorn. 🌳",
        "Keep your eyes on the prize. You're doing great! 🏆",
        "Solid foundation laid. Let's build the skyscraper! 🏢"
    )

    private val midAppsMessages = listOf(
        "Getting comfortable! The momentum is building! 🔥",
        "Look at that progress! Consistency is key. 🔑",
        "Making steady moves. The right door will open! 🚪",
        "You are making things happen. Keep at it! 💪",
        "Success is a series of daily efforts. Great job! 📈",
        "You've got a pipeline going. Keep feeding the fire! 🪵",
        "Focused and determined. You are on the right path! 🧭",
        "Keep casting your net wide. Opportunities are coming! 🎣",
        "Nearly double digits! You're building real momentum. 🌊",
        "Your pipeline is growing. Keep applying, keep believing! 🌟",
        "Each application is a lottery ticket to your future. 🎟️",
        "Consistency is the secret sauce. Keep cooking! 🍳",
        "You are taking control of your career. Keep going! 🎛️",
        "You're a job-hunting machine in the making! 🤖",
        "No day wasted. Each app brings you closer. 🎯",
        "The work you do now pays off later. Stay focused! ⏳",
        "You're creating choices for yourself. Excellent! 🎭",
        "Halfway to twenty! Keep the energy high! 🔋",
        "Action cures fear. Great job taking action! 🛡️",
        "You are outperforming your yesterday. Keep rising! ☀️"
    )

    private val largeAppsMessages = listOf(
        "Double digits! You are seriously putting in the work! 🏆",
        "Ten or more! You're building a massive net of opportunities! 🕸️",
        "Success is a numbers game, and you're playing to win! 🎲",
        "Look at you go! A true job search champion! 🎖️",
        "A stellar list of options. Keep casting that line! 🎣",
        "Double digits, double the opportunities! 🚀",
        "Your hustle is unmatched. Proud of your progress! 👏",
        "Consistency always beats luck. You've got this! 💎",
        "Ten plus apps? You're leaving nothing to chance! 🏹",
        "This is what dedication looks like. Keep it rolling! 🎡",
        "You are actively shaping your future. Keep writing! ✍️",
        "High output leads to high success. Keep applying! ⚡",
        "You are filling your pipeline like a pro! 🚰",
        "Opportunities favor the active. Keep moving! 🏃",
        "Your resolve is strong. Nothing can stop you now! 🪨",
        "Building a bright tomorrow, one app at a time. 🌅",
        "Excellent work ethic! You're creating your own luck. 🍀",
        "Hard work is a talent. You have plenty of it! 🌟",
        "Keep your foot on the gas. Success is near! 🏎️",
        "Over ten opportunities in progress! Let's get it! 💸"
    )

    private val hugeAppsMessages = listOf(
        "Absolutely unstoppable! 20+ apps is legendary! ⚡",
        "A massive pipeline! An offer is bound to land soon! 📥",
        "Outstanding hustle! Your future self is thanking you! 🌟",
        "Master of the job hunt! Incredible dedication! 👑",
        "You've cast a wide net. Get ready to reap the rewards! 🌾",
        "20+ applications! You've built a bulletproof search! 🛡️",
        "Superb effort! The universe rewards persistent action! 🌌",
        "This level of dedication is rare. Success is inevitable! 🥇",
        "You are an absolute powerhouse. Keep shining! 💡",
        "A sprawling network of possibilities. Fantastic! 🌐",
        "Twenty-plus targets! You are a force to be reckoned with. ☄️",
        "The hustle is real, and the results will be too! 💎",
        "Leaving no stone unturned. Brilliant strategy! 🪨",
        "You've put in the work. Now trust the process! 🤝",
        "A true legend of the career search! 📜",
        "Incredible endurance. You're in the final stretch! 🏁",
        "Mass scale action! You've built an empire of apps. 🏰",
        "Unwavering focus. You are an inspiration! 🌟",
        "20+ and still going? Pure determination! 💪",
        "The grand prize is coming. Keep your head high! 🏆"
    )

    fun getRandomMessage(totalCount: Int): String {
        val list = when {
            totalCount <= 0 -> zeroAppsMessages
            totalCount in 1..3 -> smallAppsMessages
            totalCount in 4..9 -> midAppsMessages
            totalCount in 10..19 -> largeAppsMessages
            else -> hugeAppsMessages
        }
        val randomIndex = Random.nextInt(list.size)
        return list[randomIndex]
    }
}

fun getRandomMotivationalMessage(totalCount: Int): String {
    return MotivationalMessages.getRandomMessage(totalCount)
}
