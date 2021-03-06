package com.maubis.scarlet.base.support.database

import android.content.Context
import com.maubis.scarlet.base.config.CoreConfig
import com.maubis.scarlet.base.core.note.NoteState
import com.maubis.scarlet.base.note.mark
import com.maubis.scarlet.base.note.save
import com.maubis.scarlet.base.settings.sheet.UISettingsOptionsBottomSheet.Companion.KEY_LIST_VIEW
import com.maubis.scarlet.base.support.ui.KEY_APP_THEME
import com.maubis.scarlet.base.support.ui.KEY_NIGHT_THEME
import com.maubis.scarlet.base.support.ui.Theme
import com.maubis.scarlet.base.utils.getLastUsedAppVersionCode

const val KEY_MIGRATE_TRASH = "KEY_MIGRATE_TRASH"
const val KEY_MIGRATE_THEME = "KEY_MIGRATE_THEME"
const val KEY_MIGRATE_DEFAULT_VALUES = "KEY_MIGRATE_DEFAULT_VALUES"
const val KEY_MIGRATE_ZERO_NOTES = "KEY_MIGRATE_ZERO_NOTES.v2"

class Migrator(val context: Context) {

  fun start() {
    runTask(KEY_MIGRATE_TRASH, {
      val notes = notesDB.getByNoteState(arrayOf(NoteState.TRASH.name))
      for (note in notes) {
        // Updates the timestamp for the note in trash
        note.mark(context, NoteState.TRASH)
      }
    })
    runTask(KEY_MIGRATE_THEME, {
      val isNightMode = CoreConfig.instance.store().get(KEY_NIGHT_THEME, true)
      CoreConfig.instance.store().put(KEY_APP_THEME, if (isNightMode) Theme.DARK.name else Theme.LIGHT.name)
      CoreConfig.instance.themeController().notifyChange(context)
    })
    runTask(KEY_MIGRATE_ZERO_NOTES, {
      val note = notesDB.getByID(0)
      if (note != null) {
        notesDB.database().delete(note)
        notesDB.notifyDelete(note)
        note.uid = null
        note.save(context)
      }
    })
    runTaskIf(
        getLastUsedAppVersionCode() == 0,
        KEY_MIGRATE_DEFAULT_VALUES, {
      CoreConfig.instance.store().put(KEY_APP_THEME, Theme.DARK.name)
      CoreConfig.instance.store().put(KEY_LIST_VIEW, true)
    })
  }

  private fun runTask(key: String, task: () -> Unit) {
    if (CoreConfig.instance.store().get(key, false)) {
      return
    }
    task()
    CoreConfig.instance.store().put(key, true)
  }

  private fun runTaskIf(condition: Boolean, key: String, task: () -> Unit) {
    if (!condition) {
      return
    }
    runTask(key, task)
  }
}