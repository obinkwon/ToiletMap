package com.oblab.toiletmap.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oblab.toiletmap.R
import com.oblab.toiletmap.ui.theme.Green
import com.oblab.toiletmap.ui.theme.GreenTint
import com.oblab.toiletmap.ui.theme.Ink
import com.oblab.toiletmap.ui.theme.SubText

enum class RegisterStep { NEED_PERMISSION, CONFIRM, FINDING, DONE }

private class StepText(val title: String, val body: String, val primary: String?, val secondary: String)

private fun textFor(step: RegisterStep) = when (step) {
    RegisterStep.NEED_PERMISSION -> StepText(
        "위치 권한이 필요해요",
        "화장실 위치를 저장하려면\n'앱 사용 중에만 허용' 이상으로 허용해 주세요.",
        "허용하기",
        "취소",
    )
    RegisterStep.CONFIRM -> StepText(
        "현재 위치를 등록할까요?",
        "지금 있는 곳을 화장실로 저장해요.\n위치와 이름은 나중에 앱에서 고칠 수 있어요.",
        "등록",
        "취소",
    )
    RegisterStep.FINDING -> StepText(
        "위치 찾는 중…",
        "잠시만 기다려 주세요.\n건물 안에서는 조금 더 걸릴 수 있어요.",
        null,
        "취소",
    )
    RegisterStep.DONE -> StepText(
        "등록했어요",
        "앱 지도에서 이름·비밀번호·메모를\n넣을 수 있어요.",
        "앱에서 보기",
        "닫기",
    )
}

/**
 * 위젯 등록 흐름의 아래쪽 카드. 홈 화면 위에 어두운 막을 깔고 카드만 띄운다.
 * [onDismiss]: 바깥 누름·취소·닫기, [onPrimary]: 허용하기·등록·앱에서 보기
 */
@Composable
fun RegisterSheet(step: RegisterStep, onDismiss: () -> Unit, onPrimary: () -> Unit) {
    val text = textFor(step)
    Box(Modifier.fillMaxSize()) {
        // 바깥을 누르면 닫힘. 위치 찾는 중엔 실수로 닫히지 않게 막는다(취소는 버튼으로)
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = step != RegisterStep.FINDING,
                    onClick = onDismiss,
                ),
        )
        Surface(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
        ) {
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StepIcon(step)
                Text(
                    text.title,
                    Modifier.padding(top = 4.dp),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    textAlign = TextAlign.Center,
                )
                Text(text.body, fontSize = 15.sp, lineHeight = 23.sp, color = SubText, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(text.secondary, color = Ink, fontSize = 16.sp)
                    }
                    if (text.primary != null) {
                        Button(
                            onClick = onPrimary,
                            modifier = Modifier.weight(2f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Green),
                        ) {
                            Text(text.primary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (step == RegisterStep.DONE) {
                    Text("3초 뒤 자동으로 닫혀요", fontSize = 12.sp, color = SubText)
                }
            }
        }
    }
}

@Composable
private fun StepIcon(step: RegisterStep) {
    val modifier = Modifier.size(64.dp)
    when (step) {
        RegisterStep.NEED_PERMISSION -> Box(modifier.background(Color(0xFFFDF0E3), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(32.dp))
        }
        RegisterStep.CONFIRM -> Box(modifier.background(GreenTint, CircleShape), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_map_pin), contentDescription = null, modifier = Modifier.size(26.dp, 34.dp))
        }
        RegisterStep.FINDING -> CircularProgressIndicator(modifier, color = Green, strokeWidth = 6.dp, trackColor = GreenTint)
        RegisterStep.DONE -> Box(modifier.background(Green, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

// 홈 화면 배경색 위에 카드를 올려서 시안과 같은 모습으로 본다
@Composable
private fun SheetPreview(step: RegisterStep) {
    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Color(0xFF2E3A35))) {
            RegisterSheet(step, onDismiss = {}, onPrimary = {})
        }
    }
}

@Preview(name = "위젯 팝업 · 권한 필요", widthDp = 390, heightDp = 844)
@Composable
private fun PermissionPreview() = SheetPreview(RegisterStep.NEED_PERMISSION)

@Preview(name = "위젯 팝업 · 등록 확인", widthDp = 390, heightDp = 844)
@Composable
private fun ConfirmPreview() = SheetPreview(RegisterStep.CONFIRM)

@Preview(name = "위젯 팝업 · 위치 찾는 중", widthDp = 390, heightDp = 844)
@Composable
private fun FindingPreview() = SheetPreview(RegisterStep.FINDING)

@Preview(name = "위젯 팝업 · 등록 완료", widthDp = 390, heightDp = 844)
@Composable
private fun DonePreview() = SheetPreview(RegisterStep.DONE)
