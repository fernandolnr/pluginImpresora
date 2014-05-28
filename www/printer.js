cordova.define("org.laneveraroja.printer", function(require, exports, module) { 
var Printer = function () {

};

Printer.prototype = {

    /**
     * Imprime un ticket de prueba
     *
     */
	testPrint: function(boolean){ 
	cordova.exec(null, null, 'Printer', 'printTestTicket', [boolean]);
	},


    /**
     * Conecta la impresora
     *
     */
    print: function (content, options) {
        var page    = content.innerHTML || content,
            options = options || {};

        if (typeof page != 'string') {
            console.log('Print function requires an HTML string. Not an object');
            return;
        }

        cordova.exec(null, null, 'Printer', 'print', [page, options]);
    }
};

var plugin = new Printer();

module.exports = plugin;
});

