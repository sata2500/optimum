package tech.salev.optimum.ui.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import tech.salev.optimum.data.model.Category

object InsightEngine {

    fun generate(
        dailyData: ImmutableList<DailyActivityData>,
        streak: Int,
        productivityPct: Int,
        categories: ImmutableList<Category>,
        categoryMinutes: Map<Long, Int>,
        totalMinutes: Int,
        avgRating: Float
    ): ImmutableList<AnalyticsInsight> {
        val insights = mutableListOf<AnalyticsInsight>()

        // 1. Streak Insight
        if (streak >= 3) {
            insights.add(
                AnalyticsInsight(
                    emoji = "🔥",
                    title = "Harika Seri!",
                    description = "$streak gündür kesintisiz kayıt yapıyorsun. Bu ivmeyi kaybetme!"
                )
            )
        } else if (streak == 0 && dailyData.isNotEmpty()) {
             insights.add(
                AnalyticsInsight(
                    emoji = "💡",
                    title = "Geri Dönme Zamanı",
                    description = "Bugün henüz zaman kaydı yapmadın. Küçük bir adımla tekrar başla."
                )
            )
        }

        // 2. Productivity Insight
        if (productivityPct >= 70) {
            insights.add(
                AnalyticsInsight(
                    emoji = "🚀",
                    title = "Üretkenlik Zirvesi",
                    description = "Zamanının %$productivityPct kadarı üretken geçti. Çok iyi bir oran!"
                )
            )
        } else if (productivityPct > 0 && productivityPct < 30) {
             insights.add(
                AnalyticsInsight(
                    emoji = "⚖️",
                    title = "Dengeyi Kur",
                    description = "Üretken zaman oranın %$productivityPct. Dinlenmek önemli ancak hedeflerine de vakit ayırdığından emin ol."
                )
            )
        }

        // 3. Category Insights (Find dominant category)
        if (totalMinutes > 0 && categoryMinutes.isNotEmpty()) {
            val dominantCatId = categoryMinutes.maxByOrNull { it.value }?.key
            if (dominantCatId != null) {
                val dominantCat = categories.find { it.id == dominantCatId }
                val dominantMinutes = categoryMinutes[dominantCatId] ?: 0
                val dominantPct = ((dominantMinutes.toFloat() / totalMinutes) * 100).toInt()
                
                if (dominantCat != null) {
                    if (dominantPct >= 40) {
                        insights.add(
                            AnalyticsInsight(
                                emoji = "📊",
                                title = "Odak Noktan: ${dominantCat.name}",
                                description = "Zamanının büyük bölümünü (%$dominantPct) bu kategoriye ayırmışsın."
                            )
                        )
                    }
                }
            }
        }

        // 4. Rating Insight
        if (avgRating >= 4.0f) {
            insights.add(
                AnalyticsInsight(
                    emoji = "⭐",
                    title = "Mutlu Günler",
                    description = "Ortalama gün değerlendirmen $avgRating. Genelde günlerinden memnunsun!"
                )
            )
        }

        // If no insights, provide a generic one
        if (insights.isEmpty()) {
            insights.add(
                AnalyticsInsight(
                    emoji = "🌱",
                    title = "Veri Toplanıyor",
                    description = "Daha fazla içgörü üretebilmemiz için zamanını kaydetmeye devam et."
                )
            )
        }

        return persistentListOf(*insights.take(3).toTypedArray()) // Show max 3 insights
    }
}
