package com.oblab.toiletmap.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.oblab.toiletmap.data.PlaceWithStats
import com.oblab.toiletmap.data.formatDistance
import com.oblab.toiletmap.ui.theme.Green
import com.oblab.toiletmap.ui.theme.Ink
import com.oblab.toiletmap.ui.theme.Line
import com.oblab.toiletmap.ui.theme.SubText

internal val DangerRed = Color(0xFFB3261E)

/** 이름·비밀번호·메모 편집. 빈 칸으로 저장하면 지운다. */
@Composable
fun EditInfoDialog(
    place: PlaceWithStats,
    onSave: (name: String, password: String, memo: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(place.name.orEmpty()) }
    var password by rememberSaveable { mutableStateOf(place.password.orEmpty()) }
    var memo by rememberSaveable { mutableStateOf(place.memo.orEmpty()) }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    AppDialog(
        title = "장소 정보 편집",
        subtitle = place.address,
        onDismissRequest = onDismiss,
        dismissText = "취소",
        onDismissClick = onDismiss,
        confirmText = "저장",
        onConfirm = { onSave(name, password, memo) },
    ) {
        InfoField("이름", name, { name = it }, "예: 역삼 센터빌딩 2층")
        InfoField(
            "비밀번호",
            password,
            { password = it },
            "도어락·화장실 비밀번호",
            // 시트처럼 기본은 가린다
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showPassword = !showPassword }) {
                    Text(if (showPassword) "숨기기" else "보기", color = SubText)
                }
            },
        )
        InfoField("메모", memo, { memo = it }, "칸 위치, 휴지 여부 등", singleLine = false)
    }
}

/** 위젯으로 등록한 장소가 이전 장소와 가까울 때. 바깥을 누르면 "나중에"(다음 실행 때 다시 묻기). */
@Composable
fun MergeDialog(
    candidate: MergeCandidate,
    remaining: Int,
    onMerge: () -> Unit,
    onKeepSeparate: () -> Unit,
    onLater: () -> Unit,
) {
    val target = candidate.target
    AppDialog(
        title = "이전 장소로 합치시겠습니까?",
        subtitle = if (remaining > 1) "남은 확인 ${remaining}건" else null,
        onDismissRequest = onLater,
        dismissText = "따로 두기",
        onDismissClick = onKeepSeparate,
        confirmText = "합치기",
        onConfirm = onMerge,
    ) {
        BodyText(
            "위젯으로 등록한 곳이 '${target.name ?: target.address ?: "이름 없음"}'에서 " +
                "${formatDistance(candidate.distanceM)} 떨어져 있습니다.\n합치면 방문 기록이 이전 장소로 옮겨집니다.",
        )
    }
}

@Composable
fun DeleteConfirmDialog(place: PlaceWithStats, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AppDialog(
        title = "장소를 삭제하시겠습니까?",
        onDismissRequest = onDismiss,
        dismissText = "취소",
        onDismissClick = onDismiss,
        confirmText = "삭제",
        onConfirm = onDelete,
        confirmColor = DangerRed,
    ) {
        BodyText("'${place.name ?: place.address ?: "이름 없음"}'과 방문 기록이 지도와 목록에서 삭제됩니다.")
    }
}

/** 앱 공통 팝업: 흰 카드 + 굵은 제목 + 아래 [보조 | 주] 버튼 반반. */
@Composable
internal fun AppDialog(
    title: String,
    onDismissRequest: () -> Unit,
    dismissText: String,
    onDismissClick: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    subtitle: String? = null,
    confirmColor: Color = Green,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                    subtitle?.let { Text(it, fontSize = 14.sp, color = SubText) }
                }

                content()

                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismissClick, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) {
                        Text(dismissText, color = Ink)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                    ) {
                        Text(confirmText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun BodyText(text: String) {
    Text(text, fontSize = 15.sp, lineHeight = 22.sp, color = Ink)
}

@Composable
private fun InfoField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, color = SubText)
        OutlinedTextField(
            value,
            onValueChange,
            Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = SubText) },
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Green,
                unfocusedBorderColor = Line,
                cursorColor = Green,
                focusedTextColor = Ink,
                unfocusedTextColor = Ink,
            ),
        )
    }
}
