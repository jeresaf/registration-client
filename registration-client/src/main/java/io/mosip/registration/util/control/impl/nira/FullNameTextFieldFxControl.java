/**
 * 
 */
package io.mosip.registration.util.control.impl.nira;

import io.mosip.commons.packet.dto.packet.SimpleDto;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.transliteration.spi.Transliteration;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.*;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.*;
import io.mosip.registration.controller.nira.GenericControllerCust;
import io.mosip.registration.controller.reg.nira.FullNameValidations;
import io.mosip.registration.controller.reg.nira.ValidationsCust;
import io.mosip.registration.dto.BlocklistedConsentDto;
import io.mosip.registration.dto.RegistrationDTO;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.util.common.DemographicChangeActionHandler;
import io.mosip.registration.util.control.FxControl;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Collectors;

import static io.mosip.registration.constants.RegistrationConstants.APPLICATION_NAME;

/**
 * @author YASWANTH S
 *
 */
public class FullNameTextFieldFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(FullNameTextFieldFxControl.class);

	private static final String loggerClassName = " Full Name Text Field Control Type Class";

	private final FullNameValidations validation;

	private final DemographicChangeActionHandler demographicChangeActionHandler;

	private Transliteration<String> transliteration;

	private Node keyboardNode;

	private static double xPosition;
	private static double yPosition;

	private final FXComponents fxComponents;

	private final GenericControllerCust genericController;

	public FullNameTextFieldFxControl() {
		ApplicationContext applicationContext = ClientApplication.getApplicationContext();
		validation = applicationContext.getBean(FullNameValidations.class);
		fxComponents = applicationContext.getBean(FXComponents.class);
		demographicChangeActionHandler = applicationContext.getBean(DemographicChangeActionHandler.class);
		Map<String, Transliteration> beans = applicationContext.getBeansOfType(Transliteration.class);
		LOGGER.debug("Transliterations implementations found : {}", beans);
		if (!beans.keySet().isEmpty()) {
			LOGGER.info("Choosing transliteration implementations --> {}", beans.keySet().iterator().next());
			this.transliteration = beans.get(beans.keySet().iterator().next());
		}
		genericController = applicationContext.getBean(GenericControllerCust.class);
		this.auditFactory = applicationContext.getBean(AuditManagerService.class);
	}

	@Override
	public FxControl build(UiFieldDTO uiFieldDTO) {
		this.uiFieldDTO = uiFieldDTO;
		this.control = this;

		VBox appLangNameVBox = create(uiFieldDTO);
		HBox hBox = new HBox();
		hBox.setSpacing(30);
		hBox.getChildren().add(appLangNameVBox);
		HBox.setHgrow(appLangNameVBox, Priority.ALWAYS);

		this.node = hBox;
		//setListener(hBox);
		return this.control;
	}

	@Override
	public void setData(Object data) {

		RegistrationDTO registrationDTO = getRegistrationDTo();
		List<SimpleDto> values = new ArrayList<SimpleDto>();

		String langCode = registrationDTO.getSelectedLanguagesByApplicant().get(0);

		TextField surnameField = (TextField) getField(uiFieldDTO.getId() + "surname" + langCode);

		TextField givenNameField = (TextField) getField(uiFieldDTO.getId() + "givenName" + langCode);

		TextField otherNamesField = (TextField) getField(uiFieldDTO.getId() + "otherNames" + langCode);

		SimpleDto simpleDto = new SimpleDto(langCode, surnameField.getText() + " " + givenNameField.getText() + " " + otherNamesField.getText());
		values.add(simpleDto);

		registrationDTO.addDemographicField(uiFieldDTO.getId(), values);
	}

	@Override
	public void setListener(Node node) {
		FXUtils.getInstance().onTypeFocusUnfocusListener((Pane) getNode(), (TextField) node);

		TextField textField = (TextField) node;

		textField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (uiFieldDTO.isTransliterate()) {
				transliterate(
					textField,
					textField.getId().substring(textField.getId().length() + uiFieldDTO.getId().length(), textField.getId().length() - RegistrationConstants.LANGCODE_LENGTH),
					textField.getId().substring(textField.getId().length() - RegistrationConstants.LANGCODE_LENGTH)
				);
			}
			if (isValid()) {				
				setData(null);

				// handling other handlers
				demographicChangeActionHandler.actionHandle((Pane) getNode(), node.getId(),
						uiFieldDTO.getChangeAction());
			} else {
				getRegistrationDTo().getDemographics().remove(this.uiFieldDTO.getId());
			}
			LOGGER.info("invoked from Listener {}",uiFieldDTO.getId());
			// Group level visibility listeners
			refreshFields();
		});
	}

	private VBox create(UiFieldDTO uiFieldDTO) {
		String fieldName = uiFieldDTO.getId();

		String langCode = getRegistrationDTo().getSelectedLanguagesByApplicant().get(0);
		ResourceBundle resourceBundle = io.mosip.registration.context.ApplicationContext.getBundle(langCode, RegistrationConstants.LABELS);

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(fieldName + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		HBox fullNamesHBox = new HBox();
		fullNamesHBox.setId(fieldName + RegistrationConstants.HBOX);
		fullNamesHBox.setSpacing(10);

		/** Adding Surname Field */
		fullNamesHBox.getChildren().add(createNameField("surname", "Surname", true, resourceBundle));

		/** Adding Given Name Field */
		fullNamesHBox.getChildren().add(createNameField("givenName", "Given Name", true, resourceBundle));

		/** Adding Other Names Field */
		fullNamesHBox.getChildren().add(createNameField("otherNames", "Other Names", false, resourceBundle));

		simpleTypeVBox.getChildren().add(fullNamesHBox);

		return simpleTypeVBox;
	}

	private VBox createNameField(String name, String label, boolean isRequired, ResourceBundle resourceBundle) {

		VBox nameVBox = new VBox();
		nameVBox.setId(uiFieldDTO.getId() + name + RegistrationConstants.VBOX);
		double prefWidth = nameVBox.getPrefWidth();
		nameVBox.setPrefWidth(prefWidth);

		/** Title label */
		Label fieldTitle = getLabel(uiFieldDTO.getId() + name + RegistrationConstants.LABEL, "",
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, true, nameVBox.getWidth());
		changeNodeOrientation(fieldTitle, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

		Label fieldMandatory = getLabel(uiFieldDTO.getId() + RegistrationConstants.IS_MANDATORY, "",
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL_ASTERISK, true, 10);

		HBox labelField = new HBox();
		labelField.getChildren().add(fieldTitle);
		labelField.getChildren().add(fieldMandatory);

		nameVBox.getChildren().add(labelField);

		String langCode = getRegistrationDTo().getSelectedLanguagesByApplicant().get(0);
		nameVBox.getChildren().add(createTextBox(langCode, name, label));
		/** Validation message (Invalid/wrong,,etc,.) */
		nameVBox.getChildren().add(getLabel(uiFieldDTO.getId() + name + langCode + RegistrationConstants.MESSAGE, null,
				RegistrationConstants.DemoGraphicFieldMessageLabel, false, nameVBox.getPrefWidth()));

		HBox hyperLinkHBox = new HBox();
		hyperLinkHBox.setVisible(false);
		hyperLinkHBox.setPrefWidth(prefWidth);
		hyperLinkHBox.setId(uiFieldDTO.getId() + name + langCode + "HyperlinkHBox");
		hyperLinkHBox.getChildren()
				.add(getHyperlink(uiFieldDTO.getId() + name + langCode + "Accept", langCode,
						io.mosip.registration.context.ApplicationContext
								.getBundle(langCode, RegistrationConstants.LABELS).getString("accept_word"), name
				));
		hyperLinkHBox.getChildren().add(getLabel(uiFieldDTO.getId() + name + langCode + "HyperlinkLabel",
				resourceBundle.getString("slash"),
				RegistrationConstants.DemoGraphicFieldMessageLabel, true, nameVBox.getPrefWidth()));
		hyperLinkHBox.getChildren()
				.add(getHyperlink(uiFieldDTO.getId() + name + langCode + "Reject", langCode,
						resourceBundle.getString("reject_word"), name
				));

		nameVBox.getChildren().add(hyperLinkHBox);

		fieldTitle.setText(label);
		fieldMandatory.setText(getMandatorySuffix(uiFieldDTO, isRequired));

		return nameVBox;
	}
	
	private Hyperlink getHyperlink(String id, String langCode, String titleText, String name) {
		/** Field Title */
		Hyperlink hyperLink = new Hyperlink();
		hyperLink.setId(id);
		hyperLink.setText(titleText);
		hyperLink.getStyleClass().add(RegistrationConstants.DemoGraphicFieldMessageLabel);
		hyperLink.setVisible(true);
		hyperLink.setWrapText(true);
		if (id.contains("Accept")) {
			hyperLink.setOnAction(event -> {
				auditFactory.audit(AuditEvent.REG_BLOCKLISTED_WORD_ACCEPTED, Components.REG_DEMO_DETAILS, SessionContext.userId(),
						AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
				
				TextField textField = (TextField) getField(uiFieldDTO.getId() + name + langCode);
				if (getRegistrationDTo().BLOCKLISTED_CHECK.containsKey(uiFieldDTO.getId())) {
					List<String> words = getRegistrationDTo().BLOCKLISTED_CHECK.get(uiFieldDTO.getId()).getWords();
					words.addAll(validation.getBlockListedWordsList(textField));
					getRegistrationDTo().BLOCKLISTED_CHECK.get(uiFieldDTO.getId()).setWords(words.stream().distinct().collect(Collectors.toList()));
				} else {
					BlocklistedConsentDto blockListedConsent = new BlocklistedConsentDto();
					blockListedConsent.setWords(validation.getBlockListedWordsList(textField));
					blockListedConsent.setOperatorConsent(true);
					blockListedConsent.setScreenName(genericController.getCurrentScreenName());
					blockListedConsent.setOperatorId(SessionContext.userId());
					getRegistrationDTo().BLOCKLISTED_CHECK.put(uiFieldDTO.getId(), blockListedConsent);
				}

				if (isValid()) {
					FXUtils.getInstance().setTextValidLabel((Pane) getNode(), textField, uiFieldDTO.getId() + name);
					setData(null);
					// handling other handlers
					demographicChangeActionHandler.actionHandle((Pane) getNode(), node.getId(),
							uiFieldDTO.getChangeAction());
				}
			});
		} else {
			hyperLink.setOnAction(event -> {
				auditFactory.audit(AuditEvent.REG_BLOCKLISTED_WORD_REJECTED, Components.REG_DEMO_DETAILS, SessionContext.userId(),
						AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
				
				TextField textField = (TextField) getField(uiFieldDTO.getId() + name + langCode);
				textField.setText(RegistrationConstants.EMPTY);
				getField(uiFieldDTO.getId() + name + langCode + "HyperlinkHBox").setVisible(false);
				if (!isValid()) {
					getRegistrationDTo().BLOCKLISTED_CHECK.remove(uiFieldDTO.getId());
					getRegistrationDTo().getDemographics().remove(this.uiFieldDTO.getId());
					FXUtils.getInstance().showErrorLabel(textField, (Pane) getNode());
				}
			});
		}
		return hyperLink;
	}

	private HBox createTextBox(String langCode, String name, String label) {
		HBox textFieldHBox = new HBox();
		TextField textField = getTextField(uiFieldDTO.getId() + name + langCode, label);
		textField.setMinWidth(200);
		textFieldHBox.getChildren().add(textField);

		HBox imagesHBox = new HBox();
		imagesHBox.getStyleClass().add(RegistrationConstants.ICONS_HBOX);
		imagesHBox.setPrefWidth(10);

		VirtualKeyboard keyBoard = new VirtualKeyboard(langCode);
		keyBoard.changeControlOfKeyboard(textField);

		ImageView keyBoardImgView = getKeyBoardImage(name);
		keyBoardImgView.setId(langCode);
		keyBoardImgView.visibleProperty().bind(textField.visibleProperty());
		keyBoardImgView.managedProperty().bind(textField.visibleProperty());

		keyBoardImgView.setOnMouseClicked((event) -> setFocusOnField(event, keyBoard, langCode, textField));
		imagesHBox.getChildren().add(keyBoardImgView);
		textFieldHBox.getChildren().add(imagesHBox);

		setListener(textField);
		changeNodeOrientation(textFieldHBox, langCode);
		ValidationsCust.putIntoLabelMap(uiFieldDTO.getId() + name + langCode, label);
		return textFieldHBox;
	}


	private TextField getTextField(String id, String label) {

		/** Text Field */
		TextField textField = new TextField();
		textField.setId(id);
		textField.setPromptText(label);
		textField.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		textField.setDisable(false);

		return textField;
	}

	private ImageView getKeyBoardImage(String name) {
		ImageView imageView = null;

		imageView = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/keyboard.png"))));
		imageView.setId(uiFieldDTO.getId() + name + "KeyBoard");
		imageView.setFitHeight(20.00);
		imageView.setFitWidth(22.00);

		return imageView;
	}

	@Override
	public Object getData() {

		return getRegistrationDTo().getDemographics().get(uiFieldDTO.getId());
	}


	@Override
	public boolean isValid() {
		boolean isValid = true;
		removeNonExistentBlockListedWords();
		String langCode = getRegistrationDTo().getSelectedLanguagesByApplicant().get(0);
		for (String name : new String[]{"surname", "givenName", "otherNames"}) {
			TextField textField = (TextField) getField(uiFieldDTO.getId() + name + langCode);
			if (textField == null)  {
				isValid = false;
				break;
			}

			getField(uiFieldDTO.getId() + name + langCode + "HyperlinkHBox").setVisible(false);
			if (validation.validateNameField((Pane) getNode(), textField, uiFieldDTO.getId() + name, true, !name.equals("otherNames"), langCode)) {
				if (validation.validateForBlockListedWords((Pane) getNode(), textField, uiFieldDTO.getId() + name, true, langCode)) {
					FXUtils.getInstance().setTextValidLabel((Pane) getNode(), textField, uiFieldDTO.getId() + name);
					getField(uiFieldDTO.getId() + name + langCode + "HyperlinkHBox").setVisible(false);
				} else {
					FXUtils.getInstance().showErrorLabel(textField, (Pane) getNode());
					if (!getField(uiFieldDTO.getId() + name + langCode + "HyperlinkHBox").isVisible()) {
						getField(uiFieldDTO.getId() + name + langCode + "HyperlinkHBox").setVisible(true);
					}
					isValid = false;
					break;
				}
			} else {
				FXUtils.getInstance().showErrorLabel(textField, (Pane) getNode());
				isValid = false;
				break;
			}
			
			if(!this.uiFieldDTO.getType().equalsIgnoreCase(RegistrationConstants.SIMPLE_TYPE)) {
				break; //not required to iterate further
			}
		}
		return isValid;
	}

	private void removeNonExistentBlockListedWords() {
		if (getRegistrationDTo().BLOCKLISTED_CHECK.containsKey(uiFieldDTO.getId()) &&
				!getRegistrationDTo().BLOCKLISTED_CHECK.get(uiFieldDTO.getId()).getWords().isEmpty()) {
			StringBuilder content = new StringBuilder();
			getRegistrationDTo().getSelectedLanguagesByApplicant().stream().forEach(langCode -> {
				TextField textField = (TextField) getField(uiFieldDTO.getId() + langCode);
				if (textField != null && textField.getText() != null) {
					content.append(textField.getText()).append(RegistrationConstants.SPACE);
				}
			});

			List<String> inputText = new ArrayList<>(Arrays.asList(content.toString().split(RegistrationConstants.SPACE)));
			if (inputText.size() > 1) inputText.add(content.toString());
			//String[] tokens = content.toString().split(RegistrationConstants.SPACE);
			getRegistrationDTo().BLOCKLISTED_CHECK.get(uiFieldDTO.getId())
					.getWords()
					.removeIf(word -> inputText.stream().noneMatch(t -> t.toLowerCase().contains(word)));
		}
	}

	@Override
	public boolean isEmpty() {
		
		List<String> langCodes = new LinkedList<String>();
		langCodes.add(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));
		
		return langCodes.stream().allMatch(langCode -> {
			TextField surnameField = (TextField) getField(uiFieldDTO.getId() + "surname" + langCode);
			TextField givenNameField = (TextField) getField(uiFieldDTO.getId() + "givenName" + langCode);

			return surnameField.getText().trim().isEmpty() || givenNameField.getText().trim().isEmpty();
		});
	}

	private void transliterate(TextField textField, String name, String langCode) {
		for (String langCodeToBeTransliterated : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
			if (!langCodeToBeTransliterated.equalsIgnoreCase(langCode)) {
				TextField textFieldToBeTransliterated = (TextField) getField(uiFieldDTO.getId() + name + langCodeToBeTransliterated);
				if (textFieldToBeTransliterated != null)  {
					try {
						textFieldToBeTransliterated.setText(transliteration.transliterate(langCode,
								langCodeToBeTransliterated, textField.getText()));
					} catch (RuntimeException runtimeException) {
						LOGGER.error(loggerClassName, APPLICATION_NAME, RegistrationConstants.APPLICATION_ID,
								"Exception occured while transliterating secondary language for field : "
										+ textField.getId()  + " due to >>>> " + runtimeException.getMessage());
					}
				}
			}
		}
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {
		return null;
	}


	@Override
	public void fillData(Object data) {
		selectAndSet(data);
	}

	@Override
	public void selectAndSet(Object data) {
		if (data == null) {
			getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(langCode -> {
				TextField textFieldSurname = (TextField) getField(
						uiFieldDTO.getId() + "surname" + langCode);
				if(textFieldSurname != null) { textFieldSurname.clear(); }
				TextField textFieldGivenName = (TextField) getField(
						uiFieldDTO.getId() + "givenName" + langCode);
				if(textFieldGivenName != null) { textFieldGivenName.clear(); }
				TextField textFieldOtherNames = (TextField) getField(
						uiFieldDTO.getId() + "OtherNames" + langCode);
				if(textFieldOtherNames != null) { textFieldOtherNames.clear(); }
				//TextField textField = (TextField) getField(uiFieldDTO.getId() + langCode);
				//if(textField != null) { textField.clear(); }
			});
			return;
		}

		if (data instanceof String) {

			String[] fullNameArray = ((String) data).split(" ");

			TextField textFieldSurname = (TextField) getField(
					uiFieldDTO.getId() + "surname" + getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));
			textFieldSurname.setText(fullNameArray.length > 0 ? fullNameArray[0] : "");
			TextField textFieldGivenName = (TextField) getField(
					uiFieldDTO.getId() + "givenName" + getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));
			textFieldGivenName.setText(fullNameArray.length > 1 ? fullNameArray[1] : "");
			TextField textFieldOtherNames = (TextField) getField(
					uiFieldDTO.getId() + "otherNames" + getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));
			textFieldOtherNames.setText(fullNameArray.length > 2 ? fullNameArray[2] : "");
			//textField.setText((String) data);
		} else if (data instanceof List) {

			List<SimpleDto> list = (List<SimpleDto>) data;

			for (SimpleDto simpleDto : list) {

				//TextField textField = (TextField) getField(uiFieldDTO.getId() + simpleDto.getLanguage());
				String fullName = simpleDto.getValue();
				String[] fullNameArray = fullName.split(" ");

				TextField textFieldSurname = (TextField) getField(
						uiFieldDTO.getId() + "surname" + simpleDto.getLanguage());
				if (textFieldSurname != null) {
					textFieldSurname.setText(fullNameArray.length > 0 ? fullNameArray[0] : "");
				}
				TextField textFieldGivenName = (TextField) getField(
						uiFieldDTO.getId() + "givenName" + simpleDto.getLanguage());
				if (textFieldGivenName != null) {
					textFieldGivenName.setText(fullNameArray.length > 1 ? fullNameArray[1] : "");
				}
				TextField textFieldOtherNames = (TextField) getField(
						uiFieldDTO.getId() + "otherNames" + simpleDto.getLanguage());
				if (textFieldOtherNames != null) {
					textFieldOtherNames.setText(fullNameArray.length > 2 ? fullNameArray[2] : "");
				}

				/*
				if (textField != null) {
					textField.setText(simpleDto.getValue());
				}
				 */
			}

		}

	}


	/**
	 *
	 * Setting the focus to specific fields when keyboard loads
	 * @param event
	 * @param keyBoard
	 * @param langCode
	 * @param textField 
	 *
	 */
	public void setFocusOnField(MouseEvent event, VirtualKeyboard keyBoard, String langCode, TextField textField) {
		try {
			Node node = (Node) event.getSource();
			node.requestFocus();
			Node parentNode = node.getParent().getParent().getParent();
			if (genericController.isKeyboardVisible()) {
				genericController.getKeyboardStage().close();
				genericController.setKeyboardVisible(false);
				if (!textField.getId().equalsIgnoreCase(genericController.getPreviousId())) {
					openKeyBoard(keyBoard, langCode, textField, parentNode);
				}
			} else {
				openKeyBoard(keyBoard, langCode, textField, parentNode);
			}
		} catch (RuntimeException runtimeException) {
			LOGGER.error(loggerClassName, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					runtimeException.getMessage() + ExceptionUtils.getStackTrace(runtimeException));
		}
	}
	
	private void openKeyBoard(VirtualKeyboard keyBoard, String langCode, TextField textField, Node parentNode) {
		if (genericController.getKeyboardStage() != null)  {
			genericController.getKeyboardStage().close();
		}
		keyboardNode = keyBoard.view();
		keyBoard.setParentStage(fxComponents.getStage());
		keyboardNode.setVisible(true);
		keyboardNode.setManaged(true);
		getField(textField.getId()).requestFocus();
		openKeyBoardPopUp();
		genericController.setPreviousId(textField.getId());
		genericController.setKeyboardVisible(true);
	}

	private GridPane prepareMainGridPaneForKeyboard() {
		GridPane gridPane = new GridPane();
		gridPane.setPrefWidth(740);

		ObservableList<ColumnConstraints> columnConstraints = gridPane.getColumnConstraints();
		ColumnConstraints columnConstraint1 = new ColumnConstraints();
		columnConstraint1.setPercentWidth(10);
		ColumnConstraints columnConstraint2 = new ColumnConstraints();
		columnConstraint2.setPercentWidth(80);
		ColumnConstraints columnConstraint3 = new ColumnConstraints();
		columnConstraint3.setPercentWidth(10);
		columnConstraints.addAll(columnConstraint1, columnConstraint2, columnConstraint3);
	
		return gridPane;
	}
	
	private void openKeyBoardPopUp() {
		try {
			Stage keyBoardStage = new Stage();
			genericController.setKeyboardStage(keyBoardStage);
			keyBoardStage.setAlwaysOnTop(true);
			keyBoardStage.initStyle(StageStyle.UNDECORATED);
			keyBoardStage.setX(300);
			keyBoardStage.setY(500);
			GridPane gridPane = prepareMainGridPaneForKeyboard();
			gridPane.addColumn(1, keyboardNode);
			Scene scene = new Scene(gridPane);
			scene.getStylesheets().add(ClassLoader.getSystemClassLoader().getResource(validation.getCssName()).toExternalForm());
			gridPane.getStyleClass().add(RegistrationConstants.KEYBOARD_PANE);
			keyBoardStage.setScene(scene);
			makeDraggable(keyBoardStage, gridPane);
			genericController.setKeyboardStage(keyBoardStage);
			keyBoardStage.show();
		} catch (Exception exception) {
			LOGGER.error(loggerClassName, APPLICATION_NAME,
					RegistrationConstants.APPLICATION_ID,
					exception.getMessage() + ExceptionUtils.getStackTrace(exception));

			validation.generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_SCAN_POPUP));
		}
	}
	
	private static void makeDraggable(final Stage stage, final Node node) {
	    node.setOnMousePressed(mouseEvent -> {
	    	// record a distance for the drag and drop operation.
	    	xPosition = stage.getX() - mouseEvent.getScreenX();
	    	yPosition = stage.getY() - mouseEvent.getScreenY();
	    	node.setCursor(Cursor.MOVE);
	    });
	    node.setOnMouseReleased(mouseEvent -> node.setCursor(Cursor.HAND));
	    node.setOnMouseDragged(mouseEvent -> {
	    	stage.setX(mouseEvent.getScreenX() + xPosition);
	    	stage.setY(mouseEvent.getScreenY() + yPosition);
	    });
	    node.setOnMouseEntered(mouseEvent -> {
	    	if (!mouseEvent.isPrimaryButtonDown()) {
	    		node.setCursor(Cursor.HAND);
	    	}
	    });
	    node.setOnMouseExited(mouseEvent -> {
	    	if (!mouseEvent.isPrimaryButtonDown()) {
	    		node.setCursor(Cursor.DEFAULT);
	    	}
	    });
	}

	protected String getMandatorySuffix(UiFieldDTO schema, boolean isRequired) {
		String mandatorySuffix = RegistrationConstants.EMPTY;
		switch (getRegistrationDTo().getFlowType()) {
			case UPDATE:
				if (getRegistrationDTo().getUpdatableFields().contains(schema.getId())) {
					mandatorySuffix = isRequired ? RegistrationConstants.ASTRIK : RegistrationConstants.EMPTY;
				}
				break;

			case CORRECTION:
			case NEW:
				mandatorySuffix = isRequired ? RegistrationConstants.ASTRIK : RegistrationConstants.EMPTY;
				break;
		}
		return mandatorySuffix;
	}
}
