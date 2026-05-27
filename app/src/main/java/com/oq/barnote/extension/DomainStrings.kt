package com.oq.barnote.extension

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.oq.barnote.R
import com.oq.barnote.core.domain.Flavor
import com.oq.barnote.core.domain.GrapeVariety
import com.oq.barnote.core.domain.NoteDetail
import com.oq.barnote.core.domain.ProductDetailInfo
import com.oq.barnote.core.domain.ProductStyle
import com.oq.barnote.core.domain.ProductType
import com.oq.barnote.core.domain.PublicScope

/**
 * iOS `Constants.swift` 의 enum `title` / `detail` 등 표시용 텍스트 매핑을 모은 extension.
 *
 * core:domain 은 순수 Kotlin 모듈이라 안드로이드 리소스에 접근할 수 없으므로,
 * stringResource lookup 은 app 레이어에서 처리합니다.
 *
 * `Composable` 함수는 `@Composable` 컨텍스트에서만 사용 가능.
 * 비-Composable 컨텍스트에서는 [titleRes] 같은 `@StringRes Int` 헬퍼와 `Context.getString` 을 사용하세요.
 */

// region ProductType -------------------------------------------------------

/**
 * iOS 의 `"\(emoji) Wine"` 등 영문 표시. iOS 가 `.localized` 를 쓰지 않으므로 이쪽도 하드코딩.
 */
fun ProductType.title(): String = "$emoji ${labelText()}"

private fun ProductType.labelText(): String = when (this) {
    ProductType.Wine -> "Wine"
    ProductType.Whisky -> "Whisky"
    ProductType.Beer -> "Beer"
    ProductType.Soju -> "Soju & Sake"
    ProductType.Liqueur -> "Liquor & Spirits"
    ProductType.Other -> "Other"
}

// endregion

// region PublicScope -------------------------------------------------------

@StringRes
fun PublicScope.titleRes(): Int = when (this) {
    PublicScope.Private -> R.string.bigonggae
    PublicScope.FriendsOnly -> R.string.cingugonggae
    PublicScope.Public -> R.string.jeoncegonggae
}

@Composable
@ReadOnlyComposable
fun PublicScope.title(): String = stringResource(titleRes())

// endregion

// region ProductDetailInfo -------------------------------------------------

@StringRes
fun ProductDetailInfo.titleRes(): Int = when (this) {
    ProductDetailInfo.Style -> R.string.seutail
    ProductDetailInfo.Grape -> R.string.pumjong
    ProductDetailInfo.Manufacturer -> R.string.jejosa
    ProductDetailInfo.Country -> R.string.weonsanji
    ProductDetailInfo.Alcohol -> R.string.dosu
    ProductDetailInfo.Ibu -> R.string.ibu
}

@Composable
@ReadOnlyComposable
fun ProductDetailInfo.title(): String = stringResource(titleRes())

// endregion

// region GrapeVariety ------------------------------------------------------

@StringRes
fun GrapeVariety.titleRes(): Int = when (this) {
    // Red
    GrapeVariety.CabernetSauvignon -> R.string.ggabereune_syobinyong
    GrapeVariety.Merlot -> R.string.mereulro
    GrapeVariety.PinotNoir -> R.string.pino_nua
    GrapeVariety.Syrah -> R.string.sira_swirajeu
    GrapeVariety.Malbec -> R.string.malbeg
    GrapeVariety.Sangiovese -> R.string.sanjiobeje
    GrapeVariety.Tempranillo -> R.string.tempeuraniyo
    GrapeVariety.Nebbiolo -> R.string.nebiolro
    GrapeVariety.Grenache -> R.string.geureunasyu
    GrapeVariety.Zinfandel -> R.string.jinpandel
    GrapeVariety.CabernetFranc -> R.string.ggabereune_peurang
    GrapeVariety.Carmenere -> R.string.ggareuminereu
    GrapeVariety.Gamay -> R.string.game
    GrapeVariety.Montepulciano -> R.string.montepulciano
    GrapeVariety.PetitVerdot -> R.string.bbeuddi_bereudo
    // White
    GrapeVariety.Chardonnay -> R.string.syareudone
    GrapeVariety.SauvignonBlanc -> R.string.syobinyong_beulrang
    GrapeVariety.Riesling -> R.string.riseulring
    GrapeVariety.PinotGrigio -> R.string.pino_geurijio
    GrapeVariety.Gewurztraminer -> R.string.gebwireuceuteuramineo
    GrapeVariety.CheninBlanc -> R.string.syunaeng_beulrang
    GrapeVariety.Viognier -> R.string.bionie
    GrapeVariety.Semillon -> R.string.semiyong
    GrapeVariety.Moscato -> R.string.moseukato
    GrapeVariety.Albarino -> R.string.albarinyo
    GrapeVariety.PinotBlanc -> R.string.pino_beulrang
    // Blend / Other
    GrapeVariety.RedBlend -> R.string.redeu_beulrendeu
    GrapeVariety.WhiteBlend -> R.string.hwaiteu_beulrendeu
    GrapeVariety.Other -> R.string.gita_pumjong
}

@Composable
@ReadOnlyComposable
fun GrapeVariety.title(): String = stringResource(titleRes())

// endregion

// region ProductStyle ------------------------------------------------------

@StringRes
fun ProductStyle.titleRes(): Int = when (this) {
    // Wine
    ProductStyle.RedWine -> R.string.redeu_wain
    ProductStyle.WhiteWine -> R.string.hwaiteu_wain
    ProductStyle.RoseWine -> R.string.roje_wain
    ProductStyle.SparklingWine -> R.string.seupakeulring_wain
    ProductStyle.DessertWine -> R.string.dijeoteu_wain
    ProductStyle.FortifiedWine -> R.string.jujeongganghwa_wain
    ProductStyle.NaturalWine -> R.string.naecureol_wain
    // Whisky
    ProductStyle.SingleMaltScotch -> R.string.singgeul_molteu_seukaci
    ProductStyle.BlendedScotch -> R.string.beulrendideu_seukaci
    ProductStyle.SingleGrainScotch -> R.string.singgeul_geurein_seukaci
    ProductStyle.Bourbon -> R.string.beobeon
    ProductStyle.RyeWhiskey -> R.string.rai_wiseuki
    ProductStyle.TennesseeWhiskey -> R.string.tenesi_wiseuki
    ProductStyle.IrishWhiskey -> R.string.airisi_wiseuki
    ProductStyle.JapaneseWhisky -> R.string.jaepaenijeu_wiseuki
    ProductStyle.CanadianWhisky -> R.string.kaenadian_wiseuki
    ProductStyle.OtherWorldWhisky -> R.string.weoldeu_wiseuki
    // Beer
    ProductStyle.Lager -> R.string.rageo
    ProductStyle.Pilsner -> R.string.pilseuneo
    ProductStyle.PaleAle -> R.string.peil_eil
    ProductStyle.Ipa -> R.string.ipa
    ProductStyle.HazyIpa -> R.string.heiji_ipa
    ProductStyle.Stout -> R.string.seutauteu
    ProductStyle.Porter -> R.string.poteo
    ProductStyle.WheatBeer -> R.string.milmaegju
    ProductStyle.SourBeer -> R.string.saweo_bieo
    ProductStyle.BelgianAle -> R.string.beljian_eil
    ProductStyle.AmberAle -> R.string.aembeo_eil
    // Soju & Sake
    ProductStyle.Soju -> R.string.soju
    ProductStyle.FruitSoju -> R.string.gwail_soju
    ProductStyle.Junmai -> R.string.junmai
    ProductStyle.JunmaiGinjo -> R.string.junmai_ginjo
    ProductStyle.JunmaiDaiginjo -> R.string.junmai_daiginjo
    ProductStyle.Ginjo -> R.string.ginjo
    ProductStyle.Daiginjo -> R.string.daiginjo
    ProductStyle.Honjozo -> R.string.honjojo
    ProductStyle.Nigori -> R.string.nigori
    ProductStyle.Cheongju -> R.string.ceongju
    ProductStyle.Yakju -> R.string.yagju
    ProductStyle.Makgeolli -> R.string.maggeolri
    // Liqueur & Spirits
    ProductStyle.Vodka -> R.string.bodeuka
    ProductStyle.Gin -> R.string.jin
    ProductStyle.LightRum -> R.string.raiteu_reom
    ProductStyle.DarkRum -> R.string.dakeu_reom
    ProductStyle.SpicedRum -> R.string.seupaiseudeu_reom
    ProductStyle.Tequila -> R.string.tekilra
    ProductStyle.Mezcal -> R.string.mejeukal
    ProductStyle.Brandy -> R.string.beuraendi
    ProductStyle.Cognac -> R.string.ggonyag
    ProductStyle.Armagnac -> R.string.areumanyag
    ProductStyle.Absinthe -> R.string.absaengteu
    ProductStyle.Baijiu -> R.string.goryangju
    ProductStyle.Liqueur -> R.string.rikyueo
    // Cocktail
    ProductStyle.ClassicCocktail -> R.string.keulraesig_kagteil
    ProductStyle.CraftCocktail -> R.string.keuraepeuteu_kagteil
    ProductStyle.TikiCocktail -> R.string.tiki_kagteil
    ProductStyle.SourCocktail -> R.string.saweo_kagteil
    ProductStyle.Highball -> R.string.haibol
    ProductStyle.FrozenCocktail -> R.string.peurojeun_kagteil
    ProductStyle.Mocktail -> R.string.mogteil
    // Coffee
    ProductStyle.Espresso -> R.string.eseupeureso
    ProductStyle.Americano -> R.string.amerikano
    ProductStyle.Latte -> R.string.radde
    ProductStyle.Cappuccino -> R.string.kapucino
    ProductStyle.Macchiato -> R.string.makiato
    ProductStyle.FlatWhite -> R.string.peulraes_hwaiteu
    ProductStyle.Mocha -> R.string.moka
    ProductStyle.DripCoffee -> R.string.deurib_keopi
    ProductStyle.PourOver -> R.string.pueo_obeo
    ProductStyle.ColdBrew -> R.string.koldeu_beuru
    ProductStyle.SingleOrigin -> R.string.singgeul_orijin
    // Other
    ProductStyle.Other -> R.string.gita
}

/** 스타일에 대한 간략 설명. iOS `ProductStyle.detail` 에 대응. 설명이 없는 케이스는 `null` 반환. */
@StringRes
fun ProductStyle.detailRes(): Int? = when (this) {
    // Wine
    ProductStyle.RedWine -> R.string.podo_ggeobjilgwa_hamgge_balhyohae_saeggwa_tanineul_cuculhan
    ProductStyle.WhiteWine -> R.string.ggeobjileul_jegeohago_gwajeubman_balhyohan_baegpodoju_gabyeo
    ProductStyle.RoseWine -> R.string.ggeobjilgwa_jjalbge_jeobcogsikyeo_bunhongbiceul_eodeun_wain
    ProductStyle.SparklingWine -> R.string.byeong_ddoneun_taengkeueseo_2ca_balhyoro_tansaneul_gajin_wai
    ProductStyle.DessertWine -> R.string.jandangi_nopa_danmasi_dudeureojineun_wain_dijeoteuwa_hamgge
    ProductStyle.FortifiedWine -> R.string.balhyo_jung_jeungryujureul_ceomgahae_dosureul_nopin_wain_pot
    ProductStyle.NaturalWine -> R.string.coesohanui_gaeibeuro_mandeun_wain_jayeon_hyomoman_sayonghago
    // Whisky
    ProductStyle.SingleMaltScotch -> R.string.seukoteulraendeu_danil_jeungryusoeseo_100_bori_maegaro_mande
    ProductStyle.BlendedScotch -> R.string.yeoreo_jeungryusoui_molteu_wiseukiwa_geurein_wiseukireul_hon
    ProductStyle.SingleGrainScotch -> R.string.seukoteulraendeu_danil_jeungryusoeseo_bori_oe_gogmuleul_sayo
    ProductStyle.Bourbon -> R.string.ogsusu_51_isang_migugeseo_sae_okeutonge_sugseonghan_wiseuki
    ProductStyle.RyeWhiskey -> R.string.homil_51_isangeuro_mandeun_wiseuki_maekomhago_deuraihan_pung
    ProductStyle.TennesseeWhiskey -> R.string.danpungnamu_suc_yeogwa_ringkeon_kaunti_peuroseseu_reul_geoci
    ProductStyle.IrishWhiskey -> R.string.juro_3hoe_jeungryuhae_budeureoun_ailraendeu_wiseuki_gabyeobg
    ProductStyle.JapaneseWhisky -> R.string.seukoteulraendeu_bangsigeul_gibaneuro_ilboneseo_mandeun_wise
    ProductStyle.CanadianWhisky -> R.string.dayanghan_gogmuleul_beulrendinghan_kaenada_wiseuki_budeureob
    // Beer
    ProductStyle.Lager -> R.string.jeooneseo_hamyeonbalhyohan_maegjuui_congcing_malggo_ggalggeu
    ProductStyle.Pilsner -> R.string.cekoeseo_sijagdoen_hwanggeumbic_rageo_seonmyeonghan_hob_hyan
    ProductStyle.PaleAle -> R.string.gurisbic_saegsange_hobgwa_molteuga_gyunhyeong_jabhin_sangmye
    ProductStyle.Ipa -> R.string.hobeul_deumbbug_sayonghae_sseunmasgwa_siteureoseu_peulroreol
    ProductStyle.HazyIpa -> R.string.taghan_oehyeonggwa_pungbuhan_yeoldaegwail_hyang_budeureobgo
    ProductStyle.Stout -> R.string.ganghage_boggeun_molteuro_mandeun_geomeun_sangmyeonbalhyo_ma
    ProductStyle.Porter -> R.string.seutauteuboda_gabyeobgo_budeureoun_heugmaegju_kaereomel_gyeo
    ProductStyle.WheatBeer -> R.string.mileul_sayonghan_heurishan_saegui_maegju_banana_jeonghyang_g
    ProductStyle.SourBeer -> R.string.jeojsan_balhyoro_sinmasi_dodeurajineun_maegju_gwaileul_deoha
    ProductStyle.BelgianAle -> R.string.belgie_hyomo_teugyuui_hyangsinryowa_gwail_hyang_nopeun_dosug
    ProductStyle.AmberAle -> R.string.gurisbic_saegsangui_gyunhyeong_jabhin_kaereomel_molteu_eil
    // Sake
    ProductStyle.Junmai -> R.string.ssal_nurug_mul_hyomomaneuro_mandeun_ceongju_yangjo_alkooleul
    ProductStyle.JunmaiGinjo -> R.string.jeongmiyul_60_iharo_ggaggeun_ssalro_mandeun_junmai_ggalggeum
    ProductStyle.JunmaiDaiginjo -> R.string.jeongmiyul_50_iharo_ggaggeun_ssalro_mandeun_junmai_hwaryeoha
    ProductStyle.Ginjo -> R.string.jeongmiyul_60_iha_yangjo_alkool_soryang_ceomga_hyangi_dodeur
    ProductStyle.Daiginjo -> R.string.jeongmiyul_50_iha_yangjo_alkool_soryang_ceomga_gajang_hwarye
    ProductStyle.Honjozo -> R.string.jeongmiyul_70_iha_yangjo_alkool_soryang_ceomga_gabyeobgo_gga
    ProductStyle.Nigori -> R.string.georeumceoneuroman_georeun_taghan_ceongju_ssalui_danmasgwa_b
    // Liqueur & Spirits
    ProductStyle.Vodka -> R.string.gogmul_gamja_deungeul_balhyohae_jeungryuhan_musaeg_muhyang_j
    ProductStyle.Gin -> R.string.junipeo_berireul_jungsimeuro_botaenikeoleul_gahyanghan_jeung
    ProductStyle.LightRum -> R.string.satangsusu_dangmilro_mandeun_ggalggeumhago_gabyeoun_reom_kag
    ProductStyle.DarkRum -> R.string.okeutongeseo_orae_sugseonghae_saegi_jitgo_pungmiga_mugjighan
    ProductStyle.SpicedRum -> R.string.reome_hyangsinryo_banilra_deungeul_gahyanghan_seutail_dalkom
    ProductStyle.Tequila -> R.string.megsikosan_beulru_agabero_mandeun_jeungryuju_ggalggeumhago_p
    ProductStyle.Mezcal -> R.string.dayanghan_agabero_mandeun_megsiko_jeontong_jeungryuju_hunyeo
    ProductStyle.Brandy -> R.string.podo_deung_gwaileul_jeungryuhan_sului_congcing_budeureoun_da
    ProductStyle.Cognac -> R.string.peurangseu_konyag_jiyeogeseo_saengsandoeneun_podo_beuraendi
    ProductStyle.Armagnac -> R.string.peurangseu_gaseukonyu_jiyeogeseo_dansig_jeungryuhan_beuraend
    ProductStyle.Absinthe -> R.string.aniseu_hoehyang_deung_heobeureul_gahyanghan_godoju_teugyuui
    ProductStyle.Baijiu -> R.string.susu_deung_gogmulro_mandeun_junggug_jeontong_jeungryuju_gang
    ProductStyle.Liqueur -> R.string.beiseu_jeungryujue_gwail_heobeu_hyangsinryo_deungeul_gahyang
    // 그 외는 detail 정의가 없음
    else -> null
}

/** InfoPopOver 상단 타이틀로 사용할 카테고리명. iOS `ProductStyle.categoryTitle` 에 대응. */
@StringRes
fun ProductStyle.categoryTitleRes(): Int? = when (category) {
    ProductStyle.Category.Wine -> R.string.wain_seutail
    ProductStyle.Category.Whisky -> R.string.wiseuki_seutail
    ProductStyle.Category.Beer -> R.string.maegju_seutail
    ProductStyle.Category.Sake -> R.string.sake_seutail
    ProductStyle.Category.Spirit -> R.string.seupiris_seutail
    null -> null
}

@Composable
@ReadOnlyComposable
fun ProductStyle.title(): String = stringResource(titleRes())

@Composable
@ReadOnlyComposable
fun ProductStyle.detail(): String = detailRes()?.let { stringResource(it) } ?: ""

@Composable
@ReadOnlyComposable
fun ProductStyle.categoryTitle(): String? = categoryTitleRes()?.let { stringResource(it) }

// endregion

// region Flavor ------------------------------------------------------------

@StringRes
fun Flavor.titleRes(): Int = when (this) {
    Flavor.TreeFruit -> R.string.namugwail
    Flavor.Berry -> R.string.beriryu
    Flavor.Citrus -> R.string.siteureoseu
    Flavor.Tropical -> R.string.yeoldaegwail
    Flavor.Floral -> R.string.peulroreol
    Flavor.Herbal -> R.string.heobeu
    Flavor.Earthy -> R.string.heulgnaeeum
    Flavor.Vanilla -> R.string.banilra
    Flavor.Chocolate -> R.string.cokolris
    Flavor.Honey -> R.string.heoni
    Flavor.Nutty -> R.string.gyeongwaryu
    Flavor.Grainy -> R.string.gogmul
    Flavor.Woody -> R.string.udi
    Flavor.Spicy -> R.string.seupaisi
    Flavor.Smoky -> R.string.seumoki
}

@StringRes
fun Flavor.detailRes(): Int = when (this) {
    Flavor.TreeFruit -> R.string.sagwa_bae_bogsunga_deung
    Flavor.Berry -> R.string.ddalgi_rajeuberi_beulruberi_deung
    Flavor.Citrus -> R.string.remon_raim_orenji
    Flavor.Tropical -> R.string.banana_painaepeul_manggo_deung
    Flavor.Floral -> R.string.ggochyanggi_jangmi_jaseumin
    Flavor.Herbal -> R.string.pul_heobeu_minteu
    Flavor.Earthy -> R.string.heulgnaeeum_beoseos_jeojeun_ip
    Flavor.Vanilla -> R.string.banilra_karamel_beoteo
    Flavor.Chocolate -> R.string.cokolris_keopi_kakao
    Flavor.Honey -> R.string.ggul_sireob_dalkomhan_hyang
    Flavor.Nutty -> R.string.amondeu_gyeongwaryu_gosoham
    Flavor.Grainy -> R.string.bori_bbang_biseukis
    Flavor.Woody -> R.string.okeu_namu_songjin
    Flavor.Spicy -> R.string.hucu_sinamon_hyangsinryo
    Flavor.Smoky -> R.string.seumoki_hunje_piteu
}

/** iOS `Flavor.title` 의 `"\(emoji) \("..".localized)"` 형식과 동일. */
@Composable
@ReadOnlyComposable
fun Flavor.title(): String = "$emoji ${stringResource(titleRes())}"

@Composable
@ReadOnlyComposable
fun Flavor.detail(): String = stringResource(detailRes())

// endregion

// region NoteDetail --------------------------------------------------------

@StringRes
fun NoteDetail.titleRes(): Int = when (this) {
    NoteDetail.Sweetness -> R.string.danmas
    NoteDetail.Acidity -> R.string.sinmas
    NoteDetail.Bitterness -> R.string.sseunmas
    NoteDetail.Body -> R.string.badigam
    NoteDetail.Tannin -> R.string.tannin
    NoteDetail.Alcoholic -> R.string.alkool
    NoteDetail.Finish -> R.string.yeoun
    NoteDetail.Aromatic -> R.string.pungmi
    NoteDetail.Balance -> R.string.baelreonseu
    NoteDetail.Feeling -> R.string.gamjeong
}

@StringRes
fun NoteDetail.detailRes(): Int? = when (this) {
    NoteDetail.Sweetness -> R.string.dangdo_dalkomhamui_jeongdo
    NoteDetail.Acidity -> R.string.sanmi_sangkeumhago_sikeumhan_jeongdo
    NoteDetail.Bitterness -> R.string.ssabssalham_sseunmasui_jeongdo
    NoteDetail.Body -> R.string.ibaneseo_neuggyeojineun_aegceui_mugegam
    NoteDetail.Tannin -> R.string.wain_deungeseo_neuggyeojineun_ddeolbeun_neuggim
    NoteDetail.Alcoholic -> R.string.dosueseo_oneun_ddeugeobgeona_ganghan_jageug
    NoteDetail.Finish -> R.string.samkin_hu_ibane_namneun_masui_gili
    NoteDetail.Aromatic -> R.string.hyanggwa_masi_eolmana_gangryeolhanji
    NoteDetail.Balance -> R.string.masui_yosodeuli_eolmana_johwarounji
    NoteDetail.Feeling -> null // iOS 도 빈 문자열
}

@Composable
@ReadOnlyComposable
fun NoteDetail.title(): String = stringResource(titleRes())

@Composable
@ReadOnlyComposable
fun NoteDetail.detail(): String = detailRes()?.let { stringResource(it) } ?: ""

// endregion

// region NoteDetail.Feeling ------------------------------------------------

@StringRes
fun NoteDetail.Feeling.descRes(): Int = when (this) {
    NoteDetail.Feeling.Happy -> R.string.gibun_joheun
    NoteDetail.Feeling.Satisfied -> R.string.manjogseureon
    NoteDetail.Feeling.Love -> R.string.banhaebeorin
    NoteDetail.Feeling.Cool -> R.string.gaeunhan
    NoteDetail.Feeling.Energetic -> R.string.hwalryeogcungjeon
    NoteDetail.Feeling.Yum -> R.string.ibmas_danggineun
    NoteDetail.Feeling.Disappointed -> R.string.silmangseureon
    NoteDetail.Feeling.Greasy -> R.string.deoburughan
    NoteDetail.Feeling.Regretful -> R.string.huhoedoeneun
    NoteDetail.Feeling.Sick -> R.string.sogi_ulreonggeorineun
}

@Composable
@ReadOnlyComposable
fun NoteDetail.Feeling.desc(): String = stringResource(descRes())

// endregion
