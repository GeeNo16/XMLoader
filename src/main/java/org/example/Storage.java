package org.example;

import java.util.HashMap;
import java.util.Map;

public class Storage {
    public static final Map<String, String> preset1 = new HashMap<>() {{
        put("extract_about_property_land.restrict_records.restrict_record.right_holders.right_holder.legal_entity.entity.resident.name", "Арендатор");
        put("extract_about_property_land.right_records.right_record.right_holders.right_holder.public_formation.public_formation_type.municipality.name", "Правообладатель орган власти");
        put("extract_about_property_land.restrict_records.restrict_record.restrictions_encumbrances_data.period.period_info.deal_validity_time", "Срок аренды");
        put("extract_about_property_land.deal_records.deal_record.deal_data.deal_type.value", "Документ основание ограничений");
        put("extract_about_property_land.land_record.object.subtype.value", "Тип земельного участка");
        put("extract_about_property_land.restrict_records.restrict_record.right_holders.right_holder.public_formation.public_formation_type.russia.name.value", "Правообладатель некоммерческая организация");
        put("extract_about_property_land.right_records.right_record.right_data.share_description", "Размеры долей");
        put("extract_about_property_land.restrict_records.restrict_record.restrictions_encumbrances_data.period.period_info.start_date", "Начало аренды");
        put("extract_about_property_land.right_records.right_record.right_holders.right_holder.legal_entity.entity.resident.name", "Правообладатель Юр лицо");
        put("extract_about_property_land.right_records.right_record.right_holders.right_holder.public_formation.public_formation_type.subject_of_rf.name.value", "Правообладатель субъект");
        put("extract_about_property_land.land_record.params.category.type.value", "Категория земель");
        put("extract_about_property_land.land_record.cad_works.cad_work.date_cadastral", "Дата");
        put("extract_about_property_land.land_record.cad_links.common_land.common_land_parts.included_cad_numbers.included_cad_number.cad_number", "Входящие в Единое землепользование контуры");
        put("extract_about_property_land.restrict_records.restrict_record.restrictions_encumbrances_data.period.period_info.end_date", "extract_about_property_land.restrict_records.restrict_record.restrictions_encumbrances_data.period.period_info.end_date");
        put("extract_about_property_land.land_record.address_location.address.readable_address", "Адрес");
        put("extract_about_property_land.land_record.params.area.value", "Площадь");
        put("extract_about_property_land.land_record.params.permitted_use.permitted_use_established.by_document", "ВРИ");
        put("extract_about_property_land.land_record.object.common_data.quarter_cad_number", "Квартал");
        put("extract_about_property_land.land_record.restrictions_encumbrances.restriction_encumbrance.cancel_date", "extract_about_property_land.land_record.restrictions_encumbrances.restriction_encumbrance.cancel_date");
        put("extract_about_property_land.restrict_records.restrict_record.right_holders.right_holder.individual.name", "Собственник Физическое Лицо");
        put("extract_about_property_land.right_records.right_record.right_holders.right_holder.individual.patronymic", "extract_about_property_land.right_records.right_record.right_holders.right_holder.individual.patronymic");
        put("extract_about_property_land.right_records.right_record.right_holders.right_holder.individual.name", "Дольщики физические лица");
        put("extract_about_property_land.land_record.object.common_data.cad_number", "Кадастровый номер");
        put("extract_about_property_land.right_records.right_record.right_data.right_type.value", "Вид права");
        put("extract_about_property_land.restrict_records.restrict_record.restrictions_encumbrances_data.restriction_encumbrance_type.value", "Ограничения");
    }};

}
