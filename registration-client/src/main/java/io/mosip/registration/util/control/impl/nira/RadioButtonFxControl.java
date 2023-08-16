package io.mosip.registration.util.control.impl.nira;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.ClientApplication;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.util.common.DemographicChangeActionHandler;
import io.mosip.registration.util.control.FxControl;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;

import java.util.*;

public class RadioButtonFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(RadioButtonFxControl.class);
	public static final String HASH = "#";
	private final DemographicChangeActionHandler demographicChangeActionHandler;
	private ToggleGroup group;

	public RadioButtonFxControl() {
		ApplicationContext applicationContext = ClientApplication.getApplicationContext();
		auditFactory = applicationContext.getBean(AuditManagerService.class);
		demographicChangeActionHandler = applicationContext.getBean(DemographicChangeActionHandler.class);
	}

	@Override
	public FxControl build(UiFieldDTO uiFieldDTO) {
		this.uiFieldDTO = uiFieldDTO;
		this.control = this;
		this.node = create(uiFieldDTO, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));
		return this.control;
	}

	private VBox create(UiFieldDTO uiFieldDTO, String langCode) {
		String fieldName = uiFieldDTO.getId();
		group = new ToggleGroup();

		/** Container holds title, fields */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(fieldName + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {
			labels.add(this.uiFieldDTO.getLabel().get(lCode));});

		/** RadioButton */
		RadioButton radioButton = getRadioButton(fieldName + "Accept",
				String.join(RegistrationConstants.SLASH, labels));
		radioButton.setToggleGroup(group);
		simpleTypeVBox.getChildren().add(radioButton);

		RadioButton radioButton2 = getRadioButton(fieldName + "Decline",
				"Decline");
		radioButton2.setToggleGroup(group);
		simpleTypeVBox.getChildren().add(radioButton2);

		setOnToggle(group);

		simpleTypeVBox.getChildren().add(getLabel(uiFieldDTO.getId() + RegistrationConstants.ERROR_MSG, null,
				RegistrationConstants.DemoGraphicFieldMessageLabel, false, simpleTypeVBox.getPrefWidth()));
		changeNodeOrientation(simpleTypeVBox, langCode);
		return simpleTypeVBox;
	}

	public void setOnToggle(ToggleGroup group) {
		group.selectedToggleProperty().addListener((ob, o, n) -> {
			RadioButton rb = (RadioButton) group.getSelectedToggle();
			if (rb != null && rb.getId().equalsIgnoreCase(this.uiFieldDTO.getId() + "Decline")) {

			}
		});
	}



	private RadioButton getRadioButton(String id, String titleText) {
		RadioButton radioButton = new RadioButton(titleText);
		radioButton.setToggleGroup(group);
		radioButton.setId(id);
		radioButton.setDisable(false);
		radioButton.setWrapText(true);
		radioButton.setPrefWidth(800);
		return radioButton;
	}


	@Override
	public void setData(Object data) {
		auditFactory.audit(AuditEvent.REG_CHECKBOX_FX_CONTROL, Components.REG_DEMO_DETAILS, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		RadioButton radioButton = (RadioButton) group.getSelectedToggle();
		getRegistrationDTo().addDemographicField(uiFieldDTO.getId(), radioButton == null ? "N" : "Y");
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDemographics().get(uiFieldDTO.getId());
	}


	@Override
	public boolean isValid() {
		if(requiredFieldValidator.isRequiredField(this.uiFieldDTO, getRegistrationDTo())){
			RadioButton radioButton = (RadioButton) group.getSelectedToggle();
			return group != null && radioButton != null && radioButton.getId().equalsIgnoreCase(this.uiFieldDTO.getId() + "Accept");
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		return group == null || group.getSelectedToggle() == null;
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {
		return null;
	}


	@Override
	public void setListener(Node node) {}

	@Override
	public void fillData(Object data) {
		selectAndSet(data);
	}

	@Override
	public void selectAndSet(Object data) {
		ObservableList<Toggle> toggles = group.getToggles();
		if(data == null) {
			group.selectToggle(null);
			return;
		}

		for (Toggle toggle : toggles) {
			if(((String) data).equals("Y")) {
				group.selectToggle(toggle);
			}
		}

	}

	/*
	private FXUtils fxUtils;

	private io.mosip.registration.context.ApplicationContext regApplicationContext;

	private ToggleGroup group;

	//private String selectedResidence = "genderSelectedButton";

	//private String residence = "genderButton";

	//private String buttonStyle = "button";

	private final MasterSyncService masterSyncService;

	public RadioButtonFxControl() {
		ApplicationContext applicationContext = ClientApplication.getApplicationContext();
		fxUtils = FXUtils.getInstance();
		regApplicationContext = io.mosip.registration.context.ApplicationContext.getInstance();
		masterSyncService = applicationContext.getBean(MasterSyncService.class);
	}

	@Override
	public FxControl build(UiFieldDTO uiFieldDTO) {
		this.uiFieldDTO = uiFieldDTO;
		this.control = this;
		this.node = create(uiFieldDTO, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

		Map<String, Object> data = new LinkedHashMap<>();
		String lang = getRegistrationDTo().getSelectedLanguagesByApplicant().get(0);
		data.put(lang, masterSyncService.getFieldValues(uiFieldDTO.getSubType(), lang, false));
		fillData(data);

		return this.control;
	}

	private VBox create(UiFieldDTO uiFieldDTO, String langCode) {
		String fieldName = uiFieldDTO.getId();

		/** Container holds title, fields and validation message elements *
		VBox simpleTypeVBox = new VBox();

		HBox hbox = new HBox();
		hbox.setId(fieldName + RegistrationConstants.HBOX);

		double prefWidth = simpleTypeVBox.getPrefWidth();

		/** Title label *
		Label fieldTitle = getLabel(fieldName + RegistrationConstants.LABEL, null, RegistrationConstants.BUTTONS_LABEL,
				true, prefWidth);
		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {
			labels.add(this.uiFieldDTO.getLabel().get(lCode));
		});

		fieldTitle.setText(String.join(RegistrationConstants.SLASH, labels)	+ getMandatorySuffix(uiFieldDTO));
		hbox.getChildren().add(fieldTitle);
		simpleTypeVBox.getChildren().add(hbox);
		simpleTypeVBox.getChildren().add(getLabel(uiFieldDTO.getId() + RegistrationConstants.ERROR_MSG, null,
				RegistrationConstants.DemoGraphicFieldMessageLabel, false, simpleTypeVBox.getPrefWidth()));
		changeNodeOrientation(simpleTypeVBox, langCode);
		return simpleTypeVBox;
	}

	@Override
	public void setData(Object data) {

		RadioButton selectedButton = getSelectedButton(group);

		if(selectedButton == null) {
			return;
		}

		String code = selectedButton.getId().replaceAll(uiFieldDTO.getId(), "");

		switch (this.uiFieldDTO.getType()) {
			case RegistrationConstants.SIMPLE_TYPE:
				List<SimpleDto> values = new ArrayList<SimpleDto>();
				List<String> toolTipText = new ArrayList<>();
				for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
					Optional<GenericDto> result = masterSyncService.getFieldValues(uiFieldDTO.getSubType(), langCode, false).stream()
							.filter(b -> b.getCode().equalsIgnoreCase(code)).findFirst();
					if (result.isPresent()) {
						values.add(new SimpleDto(langCode, result.get().getName()));
						toolTipText.add(result.get().getName());
					}
				}
				selectedButton.setTooltip(new Tooltip(String.join(RegistrationConstants.SLASH, toolTipText)));
				getRegistrationDTo().addDemographicField(uiFieldDTO.getId(), values);
				getRegistrationDTo().SELECTED_CODES.put(uiFieldDTO.getId()+"Code", code);
				break;
			default:
				Optional<GenericDto> result = masterSyncService.getFieldValues(uiFieldDTO.getSubType(), getRegistrationDTo().getSelectedLanguagesByApplicant().get(0), false).stream()
						.filter(b -> b.getCode().equalsIgnoreCase(code)).findFirst();
				if (result.isPresent()) {
					getRegistrationDTo().addDemographicField(uiFieldDTO.getId(), result.get().getName());
					getRegistrationDTo().SELECTED_CODES.put(uiFieldDTO.getId()+"Code", code);
				}
		}
	}

	private RadioButton getSelectedButton(ToggleGroup group) {
		if (group != null) {
			return (RadioButton) group.getSelectedToggle();
		}
		return null;
	}

	@Override
	public void fillData(Object data) {
		if (data != null) {
			Map<String, List<GenericDto>> val = (Map<String, List<GenericDto>>) data;
			setItems((HBox) getField(uiFieldDTO.getId() + RegistrationConstants.HBOX),
					val.get(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0)));
		}
	}

	private void setItems(HBox hBox, List<GenericDto> val) {
		if (hBox != null && val != null && !val.isEmpty()) {
			group = new ToggleGroup();
			val.forEach(genericDto -> {
				RadioButton rb = new RadioButton(genericDto.getName());
				rb.setToggleGroup(group);
				hBox.setSpacing(10);
				hBox.setPadding(new Insets(10, 10, 10, 10));
				hBox.getChildren().add(rb);
				setOnToggle(group);
			});

		}
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDemographics().get(uiFieldDTO.getId());
	}


	@Override
	public boolean isValid() {
		RadioButton selectedButton = getSelectedButton(group);
		return selectedButton != null;
	}

	@Override
	public boolean isEmpty() {
		RadioButton selectedButton = getSelectedButton(group);
		return selectedButton == null;
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {
		return null;
	}

	@Override
	public void setListener(Node node) {

	}

	public void setOnToggle(ToggleGroup group) {
		group.selectedToggleProperty().addListener((ob, o, n) -> {
			RadioButton rb = (RadioButton) group.getSelectedToggle();
			if (rb != null) {
				String s = rb.getText();

			} else {
				// show error
			}
		});
	}

	private void resetButtons(Button button) {
		//button.getStyleClass().clear();
		//button.getStyleClass().addAll(selectedResidence, buttonStyle);
		button.getParent().getChildrenUnmodifiable().forEach(node -> {
			if (node instanceof Button && !node.getId().equals(button.getId())) {
				//node.getStyleClass().clear();
				//node.getStyleClass().addAll(residence, buttonStyle);
			}
		});
	}

	@Override
	public void selectAndSet(Object data) {
		HBox hbox = (HBox) getField(uiFieldDTO.getId() + RegistrationConstants.HBOX);
		if (data == null) {
			hbox.getChildrenUnmodifiable().forEach(node -> {
				if (node instanceof Button) {
					//node.getStyleClass().clear();
					//node.getStyleClass().addAll(residence, buttonStyle);
				}
			});
			return;
		}

		Optional<Node> selectedNode;

		if (data instanceof List) {
			List<SimpleDto> list = (List<SimpleDto>) data;
			selectedNode = hbox.getChildren().stream()
					.filter(node1 -> node1.getId().equals(uiFieldDTO.getId()+(list.isEmpty()? null : list.get(0).getValue())))
					.findFirst();
		}
		else {
			selectedNode = hbox.getChildren().stream()
					.filter(node1 -> node1.getId().equals(uiFieldDTO.getId()+(String)data))
					.findFirst();
		}

		if(selectedNode.isPresent()) {
			resetButtons((Button) selectedNode.get());
		}
	}
	*/
}
