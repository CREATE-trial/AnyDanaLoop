package info.nightscout.androidaps.plugins.pump.insight.exceptions.app_layer_errors;

public class PauseModeNotAllowedException extends AppLayerErrorException {

    public PauseModeNotAllowedException(int errorCode) {
        super(errorCode);
    }
}
