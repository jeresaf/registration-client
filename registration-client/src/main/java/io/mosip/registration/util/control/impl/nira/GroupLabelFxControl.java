/**
 * 
 */
package io.mosip.registration.util.control.impl.nira;

import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.registration.dto.schema.UiFieldDTO;
import io.mosip.registration.util.control.FxControl;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author M1044402
 *
 */
public class GroupLabelFxControl extends FxControl {

	public GroupLabelFxControl() {}

	@Override
	public FxControl build(UiFieldDTO uiFieldDTO) {
		this.uiFieldDTO = uiFieldDTO;
		this.control = this;
		VBox appLangLabelVBox = create(uiFieldDTO);
		HBox hBox = new HBox();
		hBox.setSpacing(30);
		hBox.getChildren().add(appLangLabelVBox);
		HBox.setHgrow(appLangLabelVBox, Priority.ALWAYS);

		this.node = hBox;
		setListener(hBox);
		return this.control;
	}

	private VBox create(UiFieldDTO uiFieldDTO) {

		String mandatorySuffix = getMandatorySuffix(uiFieldDTO);

		String langCode = getRegistrationDTo().getSelectedLanguagesByApplicant().get(0);

		VBox ageVBox = new VBox();
		ageVBox.setPrefWidth(390);

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {
			labels.add(this.uiFieldDTO.getLabel().get(lCode));
		});

		/** Group Label */
		Label fieldTitle = getLabel(uiFieldDTO.getId() + RegistrationConstants.LABEL,
				String.join(RegistrationConstants.SLASH, labels), RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL,
				true, ageVBox.getWidth());

		Label fieldMandatory = getLabel(uiFieldDTO.getId() + RegistrationConstants.IS_MANDATORY, mandatorySuffix,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL_ASTERISK, true, 10);

		HBox labelField = new HBox();
		labelField.getChildren().add(fieldTitle);
		labelField.getChildren().add(fieldMandatory);

		ageVBox.getChildren().add(labelField);

		changeNodeOrientation(ageVBox, langCode);
		return ageVBox;
	}


	@Override
	public void setData(Object data) {}

	@Override
	public Object getData() {
		return getRegistrationDTo().getDemographics().get(uiFieldDTO.getId());
	}


	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {
		return null;
	}

	@Override
	public void setListener(Node node) {}

	@Override
	public void fillData(Object data) {
		// TODO Parse and set the date
	}

	@Override
	public void selectAndSet(Object data) {}

}
