package com.xiaomai.geek.model.article.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import com.just.agentweb.AgentWeb
import com.xiaomai.geek.R
import com.xiaomai.geek.base.BaseViewModelActivity
import com.xiaomai.geek.base.observer.BaseObserver
import com.xiaomai.geek.common.Const
import com.xiaomai.geek.common.utils.ShareUtils
import com.xiaomai.geek.db.Article
import com.xiaomai.geek.db.ArticleRecord
import com.xiaomai.geek.model.article.viewmodel.ArticleViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.geek_base_activity.*

/**
 * Created by xiaomai on 2018/2/4.
 */
class ArticleDetailActivity : BaseViewModelActivity<ArticleViewModel>() {

    private lateinit var article: Article

    private lateinit var agentWeb: AgentWeb

    private lateinit var webView: WebView

    override fun getLayoutId(): Int = 0

    override fun getViewModelClazz(): Class<ArticleViewModel> = ArticleViewModel::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        swipe_refresh_layout.isEnabled = false
        article = intent.getSerializableExtra(Const.ARTICLE) as Article

        title_view.setTitle(article.name)
        title_view.addMenu(menu = "分享", iconRes = R.drawable.menu_share, listener = View.OnClickListener {
            ShareUtils.share(this@ArticleDetailActivity, article.name, article.url)
        })
        title_view.addMenu("浏览器打开", R.drawable.menu_open_in_browser, View.OnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(article.url)
            startActivity(intent)
        })

        agentWeb = AgentWeb.with(this@ArticleDetailActivity)
                .setAgentWebParent(content_view, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT))
                .useDefaultIndicator()
                .setIndicatorColor(R.color.colorAccent)
                .createAgentWeb()
                .ready()
                .go(article.url)

        webView = agentWeb.webCreator.get()

        viewModel.loadArticleRecord(article)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : BaseObserver<ArticleRecord>() {
                    override fun onSuccess(value: ArticleRecord) {
                        webView.scrollY = value.progress.toInt()
                    }
                })
    }

    override fun showSnackBar(it: String?) {
        it?.apply {
            Snackbar.make(content_view, this, Snackbar.LENGTH_INDEFINITE)
                    .setAction("返回顶部", {
                        webView.scrollY = 0
                    }).show()
        }
    }

    override fun onResume() {
        super.onResume()
        agentWeb.webLifeCycle.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveArticleRecord(article, System.currentTimeMillis(), webView.scrollY.toFloat())
        agentWeb.webLifeCycle.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        agentWeb.webLifeCycle.onDestroy()
    }

    override fun onBackPressed() {
        if (agentWeb.back()) {
            return
        }
        super.onBackPressed()
    }
}