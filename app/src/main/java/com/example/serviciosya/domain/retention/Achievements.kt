package com.example.serviciosya.domain.retention

/** Light gamification (strategy 4): badges for the steps that make someone a regular user. */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
)

object Achievements {
    const val EXPLORER_CATEGORIES = 3

    fun of(activity: UserActivity): List<Achievement> = listOf(
        Achievement(
            id = "first_assistant",
            title = "Primera consulta",
            description = "Le contaste tu problema al asistente",
            unlocked = Milestone.ASSISTANT_USED in activity.milestones,
        ),
        Achievement(
            id = "first_contact",
            title = "Primer contacto",
            description = "Contactaste a un prestador",
            unlocked = Milestone.FIRST_CONTACT in activity.milestones,
        ),
        Achievement(
            id = "explorer",
            title = "Explorador",
            description = "Recorriste $EXPLORER_CATEGORIES categorías",
            unlocked = activity.exploredCategoryIds.size >= EXPLORER_CATEGORIES,
        ),
        Achievement(
            id = "first_favorite",
            title = "Tu prestador de confianza",
            description = "Guardaste un prestador en favoritos",
            unlocked = activity.favoriteProviderIds.isNotEmpty(),
        ),
        Achievement(
            id = "notifications_on",
            title = "Siempre al tanto",
            description = "Activaste las notificaciones",
            unlocked = Milestone.NOTIFICATIONS_ENABLED in activity.milestones,
        ),
    )

    fun unlockedIds(activity: UserActivity): Set<String> =
        of(activity).filter { it.unlocked }.mapTo(mutableSetOf()) { it.id }
}
