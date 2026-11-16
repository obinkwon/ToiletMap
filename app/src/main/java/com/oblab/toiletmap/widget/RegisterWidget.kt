package com.oblab.toiletmap.widget

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.oblab.toiletmap.R
import com.oblab.toiletmap.ToiletMapApp

/** 홈 화면 등록 위젯. 누르면 확인 얼럿([WidgetRegisterActivity])만 뜬다. */
class RegisterWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val last = (context.applicationContext as ToiletMapApp).repository.lastVisitedAt()
        val lastText = last?.let {
            "마지막 등록: " + DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        } ?: "아직 등록한 곳이 없어요"
        provideContent { Content(lastText) }
    }

    @Composable
    private fun Content(lastText: String) {
        Row(
            GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0E7C66))
                .cornerRadius(16.dp)
                .clickable(actionStartActivity<WidgetRegisterActivity>())
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // splash_icon은 가운데에 작은 핀이 있는 240dp 그림이라 크게 잡아야 핀이 보인다
            Image(ImageProvider(R.drawable.splash_icon), contentDescription = null, modifier = GlanceModifier.size(48.dp))
            Spacer(GlanceModifier.width(4.dp))
            Column {
                Text(
                    "화장실 등록",
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                )
                Text(lastText, style = TextStyle(color = ColorProvider(Color(0xFFD7EDE7)), fontSize = 12.sp), maxLines = 1)
            }
        }
    }
}

class RegisterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = RegisterWidget()
}
