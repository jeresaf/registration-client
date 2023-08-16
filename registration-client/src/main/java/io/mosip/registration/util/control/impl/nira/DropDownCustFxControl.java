package io.mosip.registration.util.control.impl.nira;

import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.ClientApplication;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.nira.GenericControllerCust;
import io.mosip.registration.controller.reg.nira.ValidationsCust;
import io.mosip.registration.dao.MasterSyncDao;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.entity.Location;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.util.common.ComboBoxAutoComplete;
import io.mosip.registration.util.common.DemographicChangeActionHandler;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.util.control.impl.DropDownFxControl;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import java.util.*;
import java.util.Map.Entry;

public class DropDownCustFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DropDownCustFxControl.class);
	private static final String loggerClassName = "DropDownFxControl";
	private int hierarchyLevel;
	private ValidationsCust validation;
	private DemographicChangeActionHandler demographicChangeActionHandler;
	private MasterSyncService masterSyncService;
	private MasterSyncDao masterSyncDao;

	public DropDownCustFxControl() {
		ApplicationContext applicationContext = ClientApplication.getApplicationContext();
		validation = applicationContext.getBean(ValidationsCust.class);
		demographicChangeActionHandler = applicationContext.getBean(DemographicChangeActionHandler.class);
		masterSyncService = applicationContext.getBean(MasterSyncService.class);
		masterSyncDao  = applicationContext.getBean(MasterSyncDao.class);
	}

	@Override
	public FxControl build(UiFieldDTO uiFieldDTO) {
		this.uiFieldDTO = uiFieldDTO;
		this.control = this;
		this.node = create(uiFieldDTO, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

		//As subType in UI Spec is defined in any lang we find the langCode to fill initial dropdown
		String subTypeLangCode = getSubTypeLangCode(uiFieldDTO.getSubType());
		if(subTypeLangCode != null) {
			TreeMap<Integer, String> groupFields = GenericControllerCust.currentHierarchyMap.getOrDefault(uiFieldDTO.getGroup(), new TreeMap<>());
			for (Entry<Integer, String> entry : GenericControllerCust.hierarchyLevels.get(subTypeLangCode).entrySet()) {
				if (entry.getValue().equals(uiFieldDTO.getSubType())) {
					this.hierarchyLevel = entry.getKey();
					groupFields.put(entry.getKey(), uiFieldDTO.getId());
					GenericControllerCust.currentHierarchyMap.put(uiFieldDTO.getGroup(), groupFields);
					break;
				}
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0),
				getPossibleValues(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0)));

		//clears & refills items
		fillData(data);
		return this.control;
	}

	private String getSubTypeLangCode(String subType) {
		for( String langCode : GenericControllerCust.hierarchyLevels.keySet()) {
			if(GenericControllerCust.hierarchyLevels.get(langCode).containsValue(subType))
				return langCode;
		}
		return null;
	}

	private VBox create(UiFieldDTO uiFieldDTO, String langCode) {
		String fieldName = uiFieldDTO.getId();

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		//simpleTypeVBox.setPrefWidth(200);
		//simpleTypeVBox.setPrefHeight(95);
		simpleTypeVBox.setSpacing(5);
		simpleTypeVBox.setId(fieldName + RegistrationConstants.VBOX);

		/** Title label */
		Label fieldTitle = getLabel(uiFieldDTO.getId() + RegistrationConstants.LABEL, "",
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, simpleTypeVBox.getWidth());

		Label fieldMandatory = getLabel(uiFieldDTO.getId() + RegistrationConstants.IS_MANDATORY, "",
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL_ASTERISK, true, 10);

		HBox labelField = new HBox();
		labelField.getChildren().add(fieldTitle);
		labelField.getChildren().add(fieldMandatory);

		simpleTypeVBox.getChildren().add(labelField);

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> labels.add(this.uiFieldDTO.getLabel().get(lCode)));

		String titleText = String.join(RegistrationConstants.SLASH, labels);
		ComboBox<GenericDto> comboBox = getComboBox(fieldName, String.join(RegistrationConstants.SLASH, labels), RegistrationConstants.DOC_COMBO_BOX,
				simpleTypeVBox.getPrefWidth(), false);
		simpleTypeVBox.getChildren().add(comboBox);

		comboBox.setOnMouseExited(event -> {
			getField(uiFieldDTO.getId() + RegistrationConstants.MESSAGE).setVisible(false);
			if(comboBox.getTooltip()!=null) {
			comboBox.getTooltip().hide();
			}
		});

		comboBox.setOnMouseEntered((event -> {
			getField(uiFieldDTO.getId() + RegistrationConstants.MESSAGE).setVisible(true);

		}));

		setListener(comboBox);

		fieldTitle.setText(titleText);
		fieldMandatory.setText(getMandatorySuffix(uiFieldDTO));
		Label messageLabel = getLabel(uiFieldDTO.getId() + RegistrationConstants.MESSAGE, null,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, simpleTypeVBox.getPrefWidth());
		messageLabel.setMaxWidth(200);
		simpleTypeVBox.getChildren().add(messageLabel);

		changeNodeOrientation(simpleTypeVBox, langCode);

		return simpleTypeVBox;
	}

	public List<GenericDto> getPossibleValues(String langCode) {
		boolean isHierarchical = false;
		String fieldSubType = uiFieldDTO.getSubType();

		if(GenericControllerCust.currentHierarchyMap.containsKey(uiFieldDTO.getGroup())) {
			isHierarchical = true;
			Entry<Integer, String> parentEntry = GenericControllerCust.currentHierarchyMap.get(uiFieldDTO.getGroup())
					.lowerEntry(this.hierarchyLevel);
			if(parentEntry == null) { //first parent
				parentEntry = GenericControllerCust.hierarchyLevels.get(langCode).lowerEntry(this.hierarchyLevel);
				Assert.notNull(parentEntry);
				List<Location> locations = masterSyncDao.getLocationDetails(parentEntry.getValue(), langCode);
				fieldSubType = locations != null && !locations.isEmpty() ? locations.get(0).getCode() : null;
			}
			else {
				FxControl fxControl = GenericControllerCust.getFxControlMap().get(parentEntry.getValue());
				Node comboBox = getField(fxControl.getNode(), parentEntry.getValue());
				GenericDto selectedItem = comboBox != null ?
						((ComboBox<GenericDto>) comboBox).getSelectionModel().getSelectedItem() : null;
				fieldSubType = selectedItem != null ? selectedItem.getCode() : null;
				if(fieldSubType == null)
					return Collections.EMPTY_LIST;
			}
		}
		return masterSyncService.getFieldValues(fieldSubType, langCode, isHierarchical);
	}

	private <T> ComboBox<GenericDto> getComboBox(String id, String titleText, String stycleClass, double prefWidth,
			boolean isDisable) {
		ComboBox<GenericDto> field = new ComboBox<GenericDto>();
		StringConverter<T> uiRenderForComboBox = FXUtils.getInstance().getStringConverterForComboBox();
		field.setId(id);
		// field.setPrefWidth(prefWidth);

		field.setPromptText(titleText);
		field.setDisable(isDisable);
		if(uiFieldDTO.getFieldLayout() != null && !uiFieldDTO.getFieldLayout().isBlank()) {
			if(Objects.equals(uiFieldDTO.getFieldLayout(), "fit-length")) {
				field.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_COMBOBOX_FULL);
			} else if(Objects.equals(uiFieldDTO.getFieldLayout(), "fit-half-length")) {
				field.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_COMBOBOX_HALF);
			}
		} else {
			field.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_COMBOBOX);
		}
		field.setConverter((StringConverter<GenericDto>) uiRenderForComboBox);
		return field;
	}

	@Override
	public void setData(Object data) {
		ComboBox<GenericDto> appComboBox = (ComboBox<GenericDto>) getField(uiFieldDTO.getId());
		if(appComboBox.getSelectionModel().getSelectedItem() == null) {
			return;
		}

		String selectedCode = appComboBox.getSelectionModel().getSelectedItem().getCode();
		switch (this.uiFieldDTO.getType()) {
			case RegistrationConstants.SIMPLE_TYPE:
				List<SimpleDto> values = new ArrayList<SimpleDto>();
				for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
					Optional<GenericDto> result = getPossibleValues(langCode).stream()
							.filter(b -> b.getCode().equals(selectedCode)).findFirst();
					if (result.isPresent()) {
						SimpleDto simpleDto = new SimpleDto(langCode, result.get().getName());
						values.add(simpleDto);
					}
				}
				getRegistrationDTo().addDemographicField(uiFieldDTO.getId(), values);
				getRegistrationDTo().SELECTED_CODES.put(uiFieldDTO.getId()+"Code", selectedCode);
				break;
			default:
				Optional<GenericDto> result = getPossibleValues(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0)).stream()
						.filter(b -> b.getCode().equals(selectedCode)).findFirst();
				if (result.isPresent()) {
					getRegistrationDTo().addDemographicField(uiFieldDTO.getId(), result.get().getName());
					getRegistrationDTo().SELECTED_CODES.put(uiFieldDTO.getId()+"Code", selectedCode);
				}
				break;
		}
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDemographics().get(uiFieldDTO.getId());
	}

	@Override
	public boolean isValid() {
		ComboBox<GenericDto> appComboBox = (ComboBox<GenericDto>) getField(uiFieldDTO.getId());
		boolean isValid = appComboBox != null && appComboBox.getSelectionModel().getSelectedItem() != null;
		if (appComboBox != null) {
			appComboBox.getStyleClass().removeIf((s) -> {
				return s.equals("demographicComboboxFocused") ||
						s.equals("demographicComboboxHalfFocused") ||
						s.equals("demographicComboboxFullFocused");
			});
			if(!isValid && uiFieldDTO.isRequired()) {
				if(uiFieldDTO.getFieldLayout() != null && !uiFieldDTO.getFieldLayout().isBlank()) {
					if(Objects.equals(uiFieldDTO.getFieldLayout(), "fit-length")) {
						appComboBox.getStyleClass().add("demographicComboboxFullFocused");
					} else if(Objects.equals(uiFieldDTO.getFieldLayout(), "fit-half-length")) {
						appComboBox.getStyleClass().add("demographicComboboxHalfFocused");
					}
				} else {
					appComboBox.getStyleClass().add("demographicComboboxFocused");
				}
			}
		}
		return isValid;
	}

	@Override
	public boolean isEmpty() {
		ComboBox<GenericDto> appComboBox = (ComboBox<GenericDto>) getField(uiFieldDTO.getId());
		return appComboBox == null || appComboBox.getSelectionModel().getSelectedItem() == null;
	}


	@Override
	public void setListener(Node node) {
		ComboBox<GenericDto> fieldComboBox = (ComboBox<GenericDto>) node;
		fieldComboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			displayFieldLabel();
			if (isValid()) {

				List<String> toolTipText = new ArrayList<>();
				String selectedCode = fieldComboBox.getSelectionModel().getSelectedItem().getCode();
				for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
					Optional<GenericDto> result = getPossibleValues(langCode).stream()
							.filter(b -> b.getCode().equals(selectedCode)).findFirst();
					if (result.isPresent()) {

						toolTipText.add(result.get().getName());
					}
				}

				Label messageLabel = (Label) getField(uiFieldDTO.getId() + RegistrationConstants.MESSAGE);
				messageLabel.setText(String.join(RegistrationConstants.SLASH, toolTipText));

				setData(null);
				refreshNextHierarchicalFxControls();
				demographicChangeActionHandler.actionHandle((Pane) getNode(), node.getId(),	uiFieldDTO.getChangeAction());
				// Group level visibility listeners
				refreshFields();
			}
		});
	}

	private void refreshNextHierarchicalFxControls() {
		if(GenericControllerCust.currentHierarchyMap.containsKey(uiFieldDTO.getGroup())) {
			Entry<Integer, String> nextEntry = GenericControllerCust.currentHierarchyMap.get(uiFieldDTO.getGroup())
					.higherEntry(this.hierarchyLevel);

			while (nextEntry != null) {
				FxControl fxControl = GenericControllerCust.getFxControlMap().get(nextEntry.getValue());
				Map<String, Object> data = new LinkedHashMap<>();
				data.put(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0),
						fxControl.getPossibleValues(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0)));

				//clears & refills items
				fxControl.fillData(data);
				nextEntry = GenericControllerCust.currentHierarchyMap.get(uiFieldDTO.getGroup())
						.higherEntry(nextEntry.getKey());
			}
		}
	}

	private void displayFieldLabel() {
		FXUtils.getInstance().toggleUIField((Pane) getNode(), uiFieldDTO.getId() + RegistrationConstants.LABEL,
				true);
		Label label = (Label) getField(uiFieldDTO.getId() + RegistrationConstants.LABEL);
		label.getStyleClass().add("demoGraphicFieldLabelOnType");
		label.getStyleClass().remove("demoGraphicFieldLabel");
		FXUtils.getInstance().toggleUIField((Pane) getNode(), uiFieldDTO.getId() + RegistrationConstants.MESSAGE, false);
	}



	private Node getField(Node fieldParentNode, String id) {
		return fieldParentNode.lookup(RegistrationConstants.HASH + id);
	}

	private void setItems(ComboBox<GenericDto> comboBox, List<GenericDto> val) {
		if (comboBox != null && val != null && !val.isEmpty()) {
			comboBox.getItems().clear();
			comboBox.getItems().addAll(val);

			new ComboBoxAutoComplete<GenericDto>(comboBox);
			
			comboBox.hide();

		}
	}

	@Override
	public void fillData(Object data) {

		if (data != null) {

			Map<String, List<GenericDto>> val = (Map<String, List<GenericDto>>) data;

			List<GenericDto> items = val.get(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

			if (items != null && !items.isEmpty()) {
				setItems((ComboBox<GenericDto>) getField(uiFieldDTO.getId()), items);
			}

		}
	}
	@Override
	public void selectAndSet(Object data) {
		ComboBox<GenericDto> field = (ComboBox<GenericDto>) getField(uiFieldDTO.getId());
		if (data == null) {
			field.getSelectionModel().clearSelection();
			return;
		}
		LOGGER.info("Dropdown {} - Data: {}", uiFieldDTO.getId(), data.toString());
		if (data instanceof List) {

			List<SimpleDto> list = (List<SimpleDto>) data;

			LOGGER.info("Dropdown Data is List: {}", list.get(0).getValue());
			selectItem(field, list.isEmpty() ? null : list.get(0).getValue());
			LOGGER.info("Dropdown Data Item Selected");

		} else if (data instanceof String) {

			selectItem(field, (String) data);
			LOGGER.info("Dropdown Data is String: {}", data);
		}
	}

	private void selectItem(ComboBox<GenericDto> field, String val) {
		if (field != null && val != null && !val.isEmpty()) {
			for (GenericDto genericDto : field.getItems()) {
				LOGGER.info("Dropdown Field Item: {}, Val: {}", genericDto.getCode(), val);
				if (genericDto.getCode().equals(val)) {
					LOGGER.info("Dropdown Field Selected Item: {}, Val: {}", genericDto.getCode(), val);
					field.getSelectionModel().select(genericDto);
					break;
				} else if (genericDto.getName().equals(val)) {
					LOGGER.info("Dropdown Field Selected Item: {}, Val: {}", genericDto.getName(), val);
					field.getSelectionModel().select(genericDto);
				}
			}
		}
	}
}
