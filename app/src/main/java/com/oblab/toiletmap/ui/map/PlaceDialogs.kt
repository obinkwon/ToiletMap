package com.oblab.toiletmap.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.oblab.toiletmap.data.PlaceWithStats

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("장소 정보 편집") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("이름") }, singleLine = true)
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("비밀번호") }, singleLine = true)
                OutlinedTextField(memo, { memo = it }, Modifier.fillMaxWidth(), label = { Text("메모") }, minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, password, memo) }) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
fun DeleteConfirmDialog(place: PlaceWithStats, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("장소를 삭제할까요?") },
        text = { Text("'${place.name ?: place.address ?: "이름 없음"}'과 방문 기록이 지도와 목록에서 사라져요.") },
        confirmButton = { TextButton(onClick = onDelete) { Text("삭제", color = Color(0xFFB3261E)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
