package soy.gabimoreno.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import soy.gabimoreno.player.service.MAIN_ACTIVITY_PENDING_INTENT
import soy.gabimoreno.presentation.MainActivity
import javax.inject.Named

@Module
@InstallIn(ServiceComponent::class)
object PlayerModule {
    @Provides
    @ServiceScoped
    @Named(MAIN_ACTIVITY_PENDING_INTENT)
    fun provideMainActivityPendingIntent(
        @ApplicationContext context: Context,
    ): PendingIntent =
        Intent(context, MainActivity::class.java).let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
}
