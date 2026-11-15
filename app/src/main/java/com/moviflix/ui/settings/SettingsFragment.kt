package com.moviflix.ui.settings

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.moviflix.R
import com.moviflix.utils.SessionManager

class SettingsFragment : Fragment() {

    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        val ivAvatar = view.findViewById<ImageView>(R.id.ivSettingsAvatar)
        val tvNickname = view.findViewById<TextView>(R.id.tvSettingsNickname)
        val tvEmail = view.findViewById<TextView>(R.id.tvSettingsEmail)
        val tvVipStatus = view.findViewById<TextView>(R.id.tvSettingsVip)
        val spinnerLang = view.findViewById<Spinner>(R.id.spinnerLanguage)
        val spinnerQuality = view.findViewById<Spinner>(R.id.spinnerQuality)
        val spinnerSubtitle = view.findViewById<Spinner>(R.id.spinnerSubtitleLang)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        // User info
        tvNickname.text = session.nickName.ifEmpty { "ضيف" }
        tvEmail.text = session.email.ifEmpty { "-" }
        tvVipStatus.text = if (session.isVip) "✓ VIP نشط" else "بدون VIP"
        Glide.with(ivAvatar)
            .load(session.headImg)
            .circleCrop()
            .placeholder(R.drawable.placeholder_avatar)
            .into(ivAvatar)

        // Language spinner
        val langs = arrayOf("ar - عربي", "en - English", "zh - 中文", "fr - Français", "tr - Türkçe")
        val langCodes = arrayOf("ar", "en", "zh", "fr", "tr")
        val langAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, langs)
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLang.adapter = langAdapter
        spinnerLang.setSelection(langCodes.indexOf(session.language).coerceAtLeast(0))
        spinnerLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                session.language = langCodes[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }

        // Quality spinner
        val qualities = arrayOf("360p (LD)", "480p (SD)", "720p (HD)", "1080p (FHD)")
        val qualCodes = arrayOf("GROOT_LD", "GROOT_SD", "GROOT_HD", "GROOT_FHD")
        val qualAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, qualities)
        qualAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerQuality.adapter = qualAdapter
        spinnerQuality.setSelection(qualCodes.indexOf(session.defaultQuality).coerceAtLeast(2))
        spinnerQuality.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                session.defaultQuality = qualCodes[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }

        // Subtitle language
        val subLangs = arrayOf("ar - عربي", "en - English", "zh - 中文", "لا توجد ترجمة")
        val subCodes = arrayOf("ar", "en", "zh", "none")
        val subAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, subLangs)
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSubtitle.adapter = subAdapter
        spinnerSubtitle.setSelection(subCodes.indexOf(session.subtitleLanguage).coerceAtLeast(0))
        spinnerSubtitle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                session.subtitleLanguage = subCodes[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }

        // Logout
        btnLogout.setOnClickListener {
            session.logout()
            requireActivity().finishAffinity()
            startActivity(android.content.Intent(requireContext(),
                com.moviflix.ui.auth.LoginActivity::class.java))
        }
    }
}
