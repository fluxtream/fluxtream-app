/*----------------------------------------------------------------------*/
/* wl_Store v 1.0 by revaxarts.com
/* description: Uses LocalStorage to save information within the Browser
/*				enviroment
/* dependency:  >IE7 :)
/*----------------------------------------------------------------------*/


$.wl_Store = function (namespace) {

		//IE7 isn't a cool client :(
	var coolClient = (typeof window.localStorage !== 'undefined' && typeof JSON !== 'undefined'),
		//namespace for the storage
		namespace = namespace || 'wl_store',


		loc = {

			//method to save data
			save: function (key, value) {
				var _save;
				if (typeof key !== 'object') {
					var _current = get() || {};
					var _obj = {};
					_obj[key] = value;
					_save = $.extend({}, _current, _obj);
				} else {
					_save = key;
				}
				localStorage[namespace] = JSON.stringify(_save);
				return true;
			},
			
			//method to get data
			get: function (key) {
				var _obj = $.parseJSON(localStorage[namespace]);

				if (typeof key !== 'undefined' && _obj) {
					return _obj[key];
				}
				return _obj;
			},
			
			//method to remove data
			remove: function (key) {
				var obj = get();
				if (typeof key !== 'undefined' && obj[key]) {
					delete obj[key];
					for (var i in obj) {
						var notempty = true;
						break;
					}
					if (notempty) {
						save(obj);
						return true;
					}
				}
				localStorage.removeItem(namespace);
				return true;
			},
			
			//delete all saved data
			flush: function () {
				localStorage.clear();
				return true;
			}
		},

		//IE 7 can't handle localStorage but cookies are to bad for storing huge data
		cok = {
			save: function (key, value) {
				return false;
			},
			get: function (key) {
				return false;
			},
			remove: function (key) {
				return false;
			},
			flush: function () {
				return false;
			}
		},
		save = function (key, value) {
			return (coolClient) ? loc.save(key, value) : cok.save(key, value);
		},

		remove = function (key) {
			return (coolClient) ? loc.remove(key) : cok.remove(key);
		},

		flush = function () {
			return (coolClient) ? loc.flush() : cok.flush();
		},

		get = function (key) {
			return (coolClient) ? loc.get(key) : cok.get(key);
		};

	//public methods
	return {
		save: function (key, value) {
			return save(key, value);
		},
		get: function (key) {
			return get(key);
		},
		remove: function (key) {
			return remove(key);
		},
		flush: function () {
			return flush();
		}

	}


};