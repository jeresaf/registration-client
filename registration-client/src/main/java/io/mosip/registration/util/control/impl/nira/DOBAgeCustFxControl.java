/**
 * 
 */
package io.mosip.registration.util.control.impl.nira;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.controller.ClientApplication;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.reg.nira.DateValidationCust;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.util.control.impl.DOBAgeFxControl;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author M1044402
 *
 */
public class DOBAgeCustFxControl extends FxControl {
	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DOBAgeCustFxControl.class);
	private static final String DOBSubType = "dateOfBirth";
	private static String loggerClassName = "DOB Age Control Type Class";
	private FXUtils fxUtils;

	private DateValidationCust dateValidation;

	public DOBAgeCustFxControl() {
		fxUtils = FXUtils.getInstance();
		ApplicationContext applicationContext = ClientApplication.getApplicationContext();
		this.dateValidation = applicationContext.getBean(DateValidationCust.class);
	}

	@Override
	public FxControl build(UiFieldDTO uiFieldDTO) {
		this.uiFieldDTO = uiFieldDTO;
		this.control = this;
		VBox appLangDateVBox = create(uiFieldDTO);
		HBox hBox = new HBox();
		hBox.setSpacing(30);
		hBox.getChildren().add(appLangDateVBox);
		HBox.setHgrow(appLangDateVBox, Priority.ALWAYS);

		this.node = hBox;
		setListener(hBox);
		return this.control;
	}

	private VBox create(UiFieldDTO uiFieldDTO) {

		HBox dobHBox = new HBox();
		dobHBox.setId(uiFieldDTO.getId() + RegistrationConstants.HBOX);
		dobHBox.setSpacing(10);

		String mandatorySuffix = getMandatorySuffix(uiFieldDTO);

		String langCode = getRegistrationDTo().getSelectedLanguagesByApplicant().get(0);
		ResourceBundle resourceBundle = io.mosip.registration.context.ApplicationContext.getBundle(langCode, RegistrationConstants.LABELS);

		VBox ageVBox = new VBox();
		ageVBox.setPrefWidth(390);

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {
			labels.add(this.uiFieldDTO.getLabel().get(lCode));
		});



		/** DOB Label */
		Label fieldTitle = getLabel(uiFieldDTO.getId() + RegistrationConstants.LABEL,
				String.join(RegistrationConstants.SLASH, labels), RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL,
				true, ageVBox.getWidth());

		Label fieldMandatory = getLabel(uiFieldDTO.getId() + RegistrationConstants.IS_MANDATORY, mandatorySuffix,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL_ASTERISK, true, 10);

		HBox labelField = new HBox();
		labelField.getChildren().add(fieldTitle);
		labelField.getChildren().add(fieldMandatory);

		ageVBox.getChildren().add(labelField);

		/** Add Date */
		dobHBox.getChildren().add(addDateTextField(uiFieldDTO, RegistrationConstants.DD,
				resourceBundle.getString(RegistrationConstants.DD)));
		/** Add Month */
		dobHBox.getChildren().add(addDateTextField(uiFieldDTO, RegistrationConstants.MM,
				resourceBundle.getString(RegistrationConstants.MM)));
		/** Add Year */
		dobHBox.getChildren().add(addDateTextField(uiFieldDTO, RegistrationConstants.YYYY,
				resourceBundle.getString(RegistrationConstants.YYYY)));

//		/** OR Label */
//		dobHBox.getChildren()
//				.add(getLabel(uiSchemaDTO.getId() + RegistrationConstants.LABEL,
//						resourceBundle.getString("ageOrDOBField"), RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true,
//						ageVBox.getWidth()));

		Label label = getLabel(uiFieldDTO.getId() + " " + RegistrationConstants.LABEL,
				resourceBundle.getString("ageOrDOBField"), RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, dobHBox.getWidth());
		label.setMinWidth(Region.USE_PREF_SIZE);
		label.setAlignment(Pos.CENTER);
		dobHBox.getChildren().add(label);
		
		/** Add Age Field */
		dobHBox.getChildren().add(addDateTextField(uiFieldDTO, RegistrationConstants.AGE_FIELD,
				resourceBundle.getString(RegistrationConstants.AGE_FIELD)));

//		/** YEARS Label */
//		dobHBox.getChildren()
//				.add(getLabel(uiSchemaDTO.getId() + RegistrationConstants.LABEL,
//						"YEARS, RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true,
//						ageVBox.getWidth()));
		ageVBox.getChildren().add(dobHBox);

		/** Validation message (Invalid/wrong,,etc,.) */
		ageVBox.getChildren().add(getLabel(uiFieldDTO.getId() + RegistrationConstants.ERROR_MSG, null,
				RegistrationConstants.DemoGraphicFieldMessageLabel, false, ageVBox.getPrefWidth()));

		dobHBox.prefWidthProperty().bind(ageVBox.widthProperty());

		changeNodeOrientation(ageVBox, langCode);
		return ageVBox;
	}

	private VBox addDateTextField(UiFieldDTO uiFieldDTO, String dd, String text) {

		VBox dateVBox = new VBox();
		dateVBox.setId(uiFieldDTO.getId() + dd + RegistrationConstants.VBOX);

		double prefWidth = dateVBox.getPrefWidth();

		/** DOB Label */
		dateVBox.getChildren().add(getLabel(uiFieldDTO.getId() + dd + RegistrationConstants.LABEL, text,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, prefWidth));


		/** DOB Text Field */
		dateVBox.getChildren().add(getTextField(uiFieldDTO.getId() + dd + RegistrationConstants.TEXT_FIELD, text,
				RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth, dd.equals(RegistrationConstants.AGE_FIELD)));

		return dateVBox;
	}

	@Override
	public void setData(Object data) {
		//LOGGER.info("Demographic data >>>>>>>>>>>>>" +data);

		TextField dd = (TextField) getField(
				uiFieldDTO.getId() + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD);
		TextField mm = (TextField) getField(
				uiFieldDTO.getId() + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD);
		TextField yyyy = (TextField) getField(
				uiFieldDTO.getId() + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD);

		getRegistrationDTo().setDateField(uiFieldDTO.getId(), dd.getText(), mm.getText(), yyyy.getText(), uiFieldDTO.getSubType());
	}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDemographics().get(uiFieldDTO.getId());
	}


	@Override
	public boolean isValid() {
		return dateValidation.validateDate((Pane) node, uiFieldDTO.getId()) && dateValidation.validateAge((Pane) node, uiFieldDTO.getId());
	}

	@Override
	public boolean isEmpty() {
		TextField dd = (TextField) getField(
				uiFieldDTO.getId() + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD);
		TextField mm = (TextField) getField(
				uiFieldDTO.getId() + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD);
		TextField yyyy = (TextField) getField(
				uiFieldDTO.getId() + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD);
		return dd != null && dd.getText().isEmpty() && mm != null && mm.getText().isEmpty() && yyyy != null
				&& yyyy.getText().isEmpty();
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {
		return null;
	}

	@Override
	public void setListener(Node node) {

		addListener(
				(TextField) getField(uiFieldDTO.getId() + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD),
				RegistrationConstants.DD);
		addListener(
				(TextField) getField(uiFieldDTO.getId() + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD),
				RegistrationConstants.MM);
		addListener(
				(TextField) getField(
						uiFieldDTO.getId() + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD),
				RegistrationConstants.YYYY);
		addListener(
				(TextField) getField(
						uiFieldDTO.getId() + RegistrationConstants.AGE_FIELD + RegistrationConstants.TEXT_FIELD),
				RegistrationConstants.AGE_FIELD);

	}

	private void addListener(TextField textField, String dateTyep) {
		textField.textProperty().addListener((ob, ov, nv) -> {

			fxUtils.toggleUIField((Pane) node,
					textField.getId().replaceAll(RegistrationConstants.TEXT_FIELD, "") + RegistrationConstants.LABEL,
					!textField.getText().isEmpty());
			if (!dateValidation.isNewValueValid(nv, dateTyep)) {
				textField.setText(ov);
			}
			boolean isValid = RegistrationConstants.AGE_FIELD.equalsIgnoreCase(dateTyep)
					? dateValidation.validateAge((Pane) node, uiFieldDTO.getId())
					: dateValidation.validateDate((Pane) node, uiFieldDTO.getId());
			if (isValid) {
				if(!RegistrationConstants.AGE_FIELD.equalsIgnoreCase(dateTyep)) {
					isValid = dateValidation.validateAge((Pane) node, uiFieldDTO.getId());
					if(isValid) {
						setData(null);
						refreshFields();
					}
				} else {
					setData(null);
					refreshFields();
				}
			}
		});
	}

	@Override
	public void fillData(Object data) {
		// TODO Parse and set the date
	}

	private TextField getTextField(String id, String titleText, String demographicTextfield, double prefWidth,
			boolean isDisable) {

		/** Text Field */
		TextField textField = new TextField();
		textField.setId(id);
		textField.setPromptText(titleText);
		textField.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		// textField.setPrefWidth(prefWidth);
		textField.setDisable(isDisable);

		return textField;
	}

	@Override
	public void selectAndSet(Object data) {
		TextField yyyy = ((TextField) getField(
				this.uiFieldDTO.getId() + RegistrationConstants.YYYY + RegistrationConstants.TEXT_FIELD));
		TextField mm = ((TextField) getField(
				this.uiFieldDTO.getId() + RegistrationConstants.MM + RegistrationConstants.TEXT_FIELD));
		TextField dd = ((TextField) getField(
				this.uiFieldDTO.getId() + RegistrationConstants.DD + RegistrationConstants.TEXT_FIELD));

		if(data == null || ((String) data).trim().isEmpty()) {
			yyyy.clear();
			mm.clear();
			dd.clear();
			return;
		}

		if (data instanceof String) {
			String[] dobArray = ((String) data).split("/");
			yyyy.setText(dobArray[0]);
			mm.setText(dobArray[1]);
			dd.setText(dobArray[2]);
		}
	}

}
