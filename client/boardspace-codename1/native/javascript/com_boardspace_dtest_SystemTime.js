(function(exports){

var o = {};

    o.currentNanoTime_ = function(callback) {
        callback.error(new Error("Not implemented yet"));
    };

    o.isSupported_ = function(callback) {
        return false;
    };

exports.com_boardspace_dtest_SystemTime= o;

})(cn1_get_native_interfaces());
