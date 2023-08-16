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
import io.mosip.registration.util.control.impl.CheckBoxFxControl;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class CheckBoxCustFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(CheckBoxCustFxControl.class);
	public static final String HASH = "#";
	private DemographicChangeActionHandler demographicChangeActionHandler;

	public CheckBoxCustFxControl() {
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

		/** Container holds title, fields */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(fieldName + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {
			labels.add(this.uiFieldDTO.getLabel().get(lCode));});

		double prefWidth = simpleTypeVBox.getPrefWidth();

		/** CheckBox */
		CheckBox checkBox = getCheckBox(fieldName,
				String.join(RegistrationConstants.SLASH, labels),
				RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth, false);

		Label fieldMandatory = getLabel(uiFieldDTO.getId() + RegistrationConstants.IS_MANDATORY, getMandatorySuffix(uiFieldDTO),
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL_ASTERISK, true, 10);

		HBox checkboxField = new HBox();
		checkboxField.getChildren().add(checkBox);
		checkboxField.getChildren().add(fieldMandatory);

		setListener(checkBox);
		simpleTypeVBox.getChildren().add(checkboxField);
		simpleTypeVBox.getChildren().add(getLabel(uiFieldDTO.getId() + RegistrationConstants.ERROR_MSG, null,
				RegistrationConstants.DemoGraphicFieldMessageLabel, false, simpleTypeVBox.getPrefWidth()));
		changeNodeOrientation(simpleTypeVBox, langCode);
		return simpleTypeVBox;
	}

	private CheckBox getCheckBox(String id, String titleText, String demographicTextfield, double prefWidth,
			boolean isDisable) {
		CheckBox checkBox = new CheckBox(titleText);
		checkBox.setId(id);
		//checkBox.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		//checkBox.setPrefWidth(prefWidth);
		checkBox.setDisable(isDisable);
		checkBox.setWrapText(true);
		checkBox.setMinWidth(200);
		checkBox.setMaxWidth(790);
		return checkBox;
	}

	@Override
	public void setData(Object data) {
		auditFactory.audit(AuditEvent.REG_CHECKBOX_FX_CONTROL, Components.REG_DEMO_DETAILS, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

		CheckBox checkBox = (CheckBox) getField(uiFieldDTO.getId());
		getRegistrationDTo().addDemographicField(uiFieldDTO.getId(), checkBox == null ? "N"
				: checkBox.isSelected() ? "Y" : "N");
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDemographics().get(uiFieldDTO.getId());
	}


	@Override
	public boolean isValid() {
		if(requiredFieldValidator.isRequiredField(this.uiFieldDTO, getRegistrationDTo())){
			CheckBox checkBox = (CheckBox) getField(uiFieldDTO.getId());
			return checkBox == null ? false : checkBox.isSelected() ? true : false;
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		CheckBox checkBox = (CheckBox) getField(uiFieldDTO.getId());
		return checkBox == null ? true : checkBox.isSelected() ? false : true;
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {
		return null;
	}


	@Override
	public void setListener(Node node) {
		CheckBox checkBox = (CheckBox) node;
		checkBox.selectedProperty().addListener((options, oldValue, newValue) -> {
			getRegistrationDTo().addDemographicField(uiFieldDTO.getId(), newValue ? "Y" : "N");
			// handling other handlers
			demographicChangeActionHandler.actionHandle((Pane) getNode(), node.getId(),
					uiFieldDTO.getChangeAction());
			// Group level visibility listeners
			refreshFields();
		});

	}

	@Override
	public void fillData(Object data) {
		selectAndSet(data);
	}

	@Override
	public void selectAndSet(Object data) {
		CheckBox checkBox = (CheckBox) getField(uiFieldDTO.getId());
		if(data == null) {
			checkBox.setSelected(false);
			return;
		}

		checkBox.setSelected(data != null && ((String)data).equals("Y") ? true : false);
	}

}
