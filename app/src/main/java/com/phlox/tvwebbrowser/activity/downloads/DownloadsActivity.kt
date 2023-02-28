package com.phlox.tvwebbrowser.activity.downloads

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.coccocbrowsejavatest.BuildConfig
import com.example.coccocbrowsejavatest.R
import com.example.coccocbrowsejavatest.databinding.ActivityDownloadsBinding
import com.phlox.tvwebbrowser.model.Download
import com.phlox.tvwebbrowser.utils.Utils
import com.phlox.tvwebbrowser.utils.activemodel.ActiveModelsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DownloadsActivity : AppCompatActivity(), AdapterView.OnItemClickListener, ActiveDownloadsModel.Listener, AdapterView.OnItemLongClickListener{
    private lateinit var vb: ActivityDownloadsBinding
    private lateinit var adapter: DownloadListAdapter
    private val listeners = ArrayList<ActiveDownloadsModel.Listener>()

    private lateinit var activeDownloadsModel: ActiveDownloadsModel
    private lateinit var downloadsHistoryModel: DownloadsHistoryModel

    internal var onListScrollListener: AbsListView.OnScrollListener = object : AbsListView.OnScrollListener {
        override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {

        }

        override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
            if (totalItemCount != 0 && firstVisibleItem + visibleItemCount >= totalItemCount - 1) {
                downloadsHistoryModel.loadNextItems()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate this:" + System.identityHashCode(this))
        vb = ActivityDownloadsBinding.inflate(layoutInflater)
        setContentView(vb.root)

        activeDownloadsModel = ActiveModelsRepository.get(ActiveDownloadsModel::class, this)
        downloadsHistoryModel = ActiveModelsRepository.get(DownloadsHistoryModel::class, this)

        adapter = DownloadListAdapter(this)
        vb.listView.adapter = adapter

        vb.listView.setOnScrollListener(onListScrollListener)
        vb.listView.onItemClickListener = this
        vb.listView.onItemLongClickListener = this

        downloadsHistoryModel.lastLoadedItems.subscribe(this, false, {
            if (it.isNotEmpty()) {
                vb.tvPlaceholder.visibility = View.GONE
                adapter.addItems(it)
                vb.listView.requestFocus()
            }
        })

        if (downloadsHistoryModel.allItems.isEmpty()) {
            downloadsHistoryModel.loadNextItems()
        } else {
            vb.tvPlaceholder.visibility = View.GONE
            adapter.addItems(downloadsHistoryModel.allItems)
            vb.listView.requestFocus()
        }
    }

    override fun onResume() {
        super.onResume()
        activeDownloadsModel.registerListener(this)
    }

    override fun onPause() {
        activeDownloadsModel.unregisterListener(this@DownloadsActivity)
        super.onPause()
    }

    override fun onItemClick(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
        val v = view as DownloadListItemView
        val download = v.download ?: return

        if (download.isDateHeader) {
            return
        }

        if (download.size != download.bytesReceived) {
            //file should be already marked in UI as broken
            return
        }


        val fileURI = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Uri.parse(download.filepath)
        } else {
            val file = File(download.filepath)
            if (!file.exists()) {
                Utils.showToast(this, R.string.file_not_found)
                return
            }

            FileProvider.getUriForFile(this@DownloadsActivity,
                BuildConfig.APPLICATION_ID + ".provider",
                file)
        }


        val openIntent = Intent(Intent.ACTION_VIEW)
        val mimeType = contentResolver.getType(fileURI)
        openIntent.setDataAndType(fileURI, mimeType)
        openIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            startActivity(openIntent)
        } catch (e: ActivityNotFoundException) {
            Utils.showToast(this, getString(R.string.no_app_for_file_type))
        }

    }

    override fun onItemLongClick(adapterView: AdapterView<*>, view: View, i: Int, l: Long): Boolean {
        val v = view as DownloadListItemView
        val download = v.download ?: return false
        if (download.isDateHeader) {
            return true
        }
        if (download.size == Download.BROKEN_MARK || download.size == Download.CANCELLED_MARK ||
                download.size == download.bytesReceived) {
            showFinishedDownloadOptionsPopup(v)
        } else {
            showUnfinishedDownloadOptionsPopup(v)
        }
        return true
    }

    private fun showUnfinishedDownloadOptionsPopup(v: DownloadListItemView) {
        val pm = PopupMenu(this, v, Gravity.BOTTOM)
        pm.menu.add(R.string.cancel)
        pm.setOnMenuItemClickListener {
            activeDownloadsModel.cancelDownload(v.download!!)
            true
        }
        pm.show()
    }

    private fun showFinishedDownloadOptionsPopup(v: DownloadListItemView) {
        val download = v.download ?: return
        val pm = PopupMenu(this, v, Gravity.BOTTOM)
        if (download.filename.endsWith(".apk", true) &&
            download.size == download.bytesReceived) {
            pm.menu.add(0, 0, 0, R.string.install)
        }
        pm.menu.add(0, 1, 1, R.string.open_folder)
        pm.menu.add(0, 2, 2, R.string.delete)
        pm.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                0 -> installAPK(download)
                1 -> openFolder(v)
                2 -> deleteItem(v)
            }
            true
        }
        pm.show()
    }

    private fun installAPK(download: Download) {
        val canInstallFromOtherSources = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //packageManager.canRequestPackageInstalls()
            true
        } else
            Settings.Secure.getInt(this.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS) == 1

        if(canInstallFromOtherSources) {
            launchInstallAPKActivity(this, download)
        } else {
            AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        R.string.turn_on_unknown_sources_for_app else R.string.turn_on_unknown_sources)
                    .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { dialog, which -> run {
                        val intentSettings = Intent()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            intentSettings.action = Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                            intentSettings.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                        } else {
                            intentSettings.action = Settings.ACTION_SECURITY_SETTINGS
                        }
                        intentSettings.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        try {
                            startActivityForResult(intentSettings, REQUEST_CODE_UNKNOWN_APP_SOURCES)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show()
                        }
                    }})
                    .show()

        }
    }

    private fun openFolder(v: DownloadListItemView) {
        val uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "resource/folder")
        if (intent.resolveActivityInfo(packageManager, 0) != null) {
            startActivity(intent)
        } else {
            Utils.showToast(this, R.string.no_file_explorer_msg)
        }
    }

    private fun deleteItem(v: DownloadListItemView) = lifecycleScope.launch(Dispatchers.Main) {
        v.download?.let {
            activeDownloadsModel.deleteItem(it)
            adapter.remove(it)
        }
    }

    override fun onDownloadUpdated(downloadInfo: Download) {
        for (i in listeners.indices) {
            listeners[i].onDownloadUpdated(downloadInfo)
        }
    }

    override fun onDownloadError(downloadInfo: Download, responseCode: Int, responseMessage: String) {
        for (i in listeners.indices) {
            listeners[i].onDownloadError(downloadInfo, responseCode, responseMessage)
        }
    }

    override fun onAllDownloadsComplete() {}

    fun registerListener(listener: ActiveDownloadsModel.Listener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: ActiveDownloadsModel.Listener) {
        listeners.remove(listener)
    }

    companion object {
        const val REQUEST_CODE_UNKNOWN_APP_SOURCES = 10007
        private const val REQUEST_CODE_INSTALL_PACKAGE = 10008
        val TAG: String = DownloadsActivity::class.java.simpleName

        fun launchInstallAPKActivity(activity: Activity, download: Download) {
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("apk")
            val apkURI = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Uri.parse(download.filepath)
            } else {
                val file = File(download.filepath)
                FileProvider.getUriForFile(
                    activity,
                    activity.applicationContext.packageName + ".provider", file
                )
            }

            val install = Intent(Intent.ACTION_INSTALL_PACKAGE)
            install.setDataAndType(apkURI, mimeType)
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try {
                activity.startActivityForResult(install, REQUEST_CODE_INSTALL_PACKAGE)//we are not using result for now
            } catch (e: Exception) {
                Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
