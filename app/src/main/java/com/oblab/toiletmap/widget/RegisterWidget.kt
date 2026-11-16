package com.oblab.toiletmap.widget

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.oblab.toiletmap.R
import com.oblab.toiletmap.ToiletMapApp

private val Green = Color(0xFF0E7C66)
private val Ink = Color(0xFF1C211E)
private val SubText = Color(0xFF5F6660)

/**
 * 홈 화면 등록 위젯. 누르면 확인 카드([WidgetRegisterActivity])만 뜬다.
 * 2x1 이상은 흰 카드(B안), 1x1은 초록 아이콘형(C안). 둥근 모서리는 Android 12+에서만 보인다.
 */
class RegisterWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val last = (context.applicationContext as ToiletMapApp).repository.lastVisitedAt()
        val lastText = last?.let {
            "마지막 · " + DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        } ?: "아직 등록한 곳이 없어요"
        provideContent {
            if (LocalSize.current.width >= WIDE.width) Wide(lastText) else Small()
        }
    }

    /** B안: 흰 카드, 왼쪽 글자 + 오른쪽 초록 [+] */
    @SuppressLint("RestrictedApi")
    @Composable
    private fun Wide(lastText: String) {
        Row(
            GlanceModifier
                .fillMaxSize()
                .background(Color.White)
                .cornerRadius(22.dp)
                .clickable(actionStartActivity<WidgetRegisterActivity>())
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(GlanceModifier.defaultWeight()) {
                Text("화장실 등록", style = TextStyle(color = ColorProvider(Ink), fontSize = 16.sp, fontWeight = FontWeight.Bold))
                Text(lastText, style = TextStyle(color = ColorProvider(SubText), fontSize = 12.sp), maxLines = 1)
            }
            Box(
                GlanceModifier.size(52.dp).background(Green).cornerRadius(26.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(ImageProvider(R.drawable.ic_widget_add), contentDescription = "등록", modifier = GlanceModifier.size(26.dp))
            }
        }
    }

    /** C안: 1x1 초록 아이콘형. 칸이 작아 마지막 등록 시각은 뺀다 */
    @SuppressLint("RestrictedApi")
    @Composable
    private fun Small() {
        Column(
            GlanceModifier
                .fillMaxSize()
                .background(Green)
                .cornerRadius(22.dp)
                .clickable(actionStartActivity<WidgetRegisterActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(ImageProvider(R.drawable.ic_widget_pin), contentDescription = null, modifier = GlanceModifier.size(26.dp, 34.dp))
            Spacer(GlanceModifier.height(4.dp))
            Text("등록", style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
        }
    }

    companion object {
        private val SMALL = DpSize(40.dp, 40.dp)
        private val WIDE = DpSize(110.dp, 40.dp)
    }
}

class RegisterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = RegisterWidget()
}
