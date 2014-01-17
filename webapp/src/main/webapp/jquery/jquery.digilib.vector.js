/*
 * #%L
 * digilib vector plugin
 * %%
 * Copyright (C) 2014 MPIWG Berlin, Bibliotheca Hertziana
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 * Authors: Robert Casties, Martin Raspe
 */

/**
 * digilib vector plugin.
 * 
 * Displays vector shapes on top of the image.
 * 
 * Shapes are objects with "geometry" and "properties" members.
 * Geometry is an object with "type" and "coordinates" members.
 * Types: Line, Rectangle. Coordinates are lists of pairs of relative coordinates.
 * Properties are SVG properties "stroke", "stroke-width", "fill" and other properties.
 * If a shape has an "id" member its value will be used in SVG.
 * 
 * shape = {
 *   'geometry' : {
 *     'type' : 'Line',
 *     'coordinates' : [[0.1, 0.2], [0.3, 0.4]]
 *   },
 *   'properties' : {
 *     'stroke' : 'blue'
 *   }
 * }
 * 
 */
(function($) {

    // affine geometry
    var geom = null;
    // plugin object with digilib data
    var digilib = null;
    // SVG namespace
    var svgNS = 'http://www.w3.org/2000/svg';
    
    
    var buttons = {
    };

    var defaults = {
        // is vector active?
        'isVectorActive' : true,
        // default SVG stroke
        'defaultStroke' : 'red',
        // default SVG stroke-width
        'defaultStrokeWidth' : '2',
        // default SVG fill
        'defaultFill' : 'none',
        // grab handle size
        'editHandleSize' : 10
    };

    var actions = {
        /**
         * set list of vector objects (shapes).
         * 
         * replaces existing shapes.
         * 
         * @param data
         * @param shapes
         */
        setShapes : function(data, shapes) {
        	data.shapes = shapes;
        	renderShapes(data);
        },
	
        /**
         * add vector object (shape) or create one by clicking.
         * 
         * For interactive use shape has to be initialized with a shape object with
         * type but no coordinates, e.g {'geometry':{'type':'Line'}}. The onComplete
         * function will be called with data and the new shape object as parameters.
         * 
         * @param data
         * @param shape
         * @param onComplete
         */
        addShape : function(data, shape, onComplete) {
        	if (data.shapes == null) {
        		data.shapes = [];
        	};
        	if (shape.geometry.coordinates == null) {
        		// define shape interactively
        		defineShape(data, shape, onComplete);
        	} else {
        		data.shapes.push(shape);
            	renderShapes(data);
        	}
        },
        
        /**
         * get vector object (shape) by id.
         * 
         * @param data
         * @param id
         * @returns shape
         */
        getShapeById : function(data, id) {
        	shapes = data.shapes;
        	if (shapes == null) return null;
        	for (var i in shapes) {
        		if (shapes[i].id === id) {
        			return shapes[i];
        		}
        	}
        	return null;
        },
        
        /**
         * remove vector object (shape) by id.
         * 
         * @param data
         * @param id
         */
        removeShapeById : function(data, id) {
        	shapes = data.shapes;
        	if (shapes == null) return;
        	for (var i in shapes) {
        		if (shapes[i].id === id) {
        			shapes.splice(i, 1);
        		}
        	}
        	displayShapes(data);
        }        	
    };

    // plugin installation routine, called by digilib on each plugin object.
    var install = function(plugin) {
        digilib = plugin;
        console.debug('installing vector plugin. digilib:', digilib);
        // import geometry classes
        geom = digilib.fn.geometry;
        // add defaults, actions, buttons to the main digilib object
        $.extend(digilib.defaults, defaults);
        $.extend(digilib.actions, actions);
        $.extend(digilib.buttons, buttons);
    };

    // plugin initialization
    var init = function (data) {
        console.debug('initialising vector plugin. data:', data);
        var $data = $(data);
        // install event handlers
        $data.bind('setup', handleSetup);
        $data.bind('update', handleUpdate);
    };


    var handleSetup = function (evt) {
        console.debug("vector: handleSetup");
        var data = this;
        //renderShapes(data);
    };

    /**
     * render list of shapes on screen.
     */
    var renderShapes = function (data) {
    	console.debug("renderShapes shapes:", data.shapes);
    	if (data.shapes == null || data.imgTrafo == null || !data.settings.isVectorActive) 
    	    return;
        if (data.$svg != null) {
        	data.$svg.remove();
        }
        var settings = data.settings;
    	var $svg = $(createSvg('svg', {
    	    'viewBox': data.imgRect.getAsSvg(),
    	    'class': settings.cssPrefix+'overlay',
    		'style': 'position:absolute; z-index:10; pointer-events:none;'}));
        // adjust svg element size and position (doesn't work with .adjustDiv())
        $svg.css(data.imgRect.getAsCss());
        data.$svg = $svg;
    	for (var i in data.shapes) {
    		var shape = data.shapes[i];
    		renderShape(data, shape, $svg);
    	}
    	data.$elem.append($svg);
    };
    
    /**
     * render a shape on screen.
     * 
     * Creates a SVG element and adds it to $svg.
     * Puts a reference to the element in the shape object.
     */
    var renderShape = function (data, shape, $svg) {
        if ($svg == null) {
            if (data.$svg == null) {
                renderShapes(data);
            }
            $svg = data.$svg;
        }
        var settings = data.settings;
        var css = settings.cssPrefix;
        var hs = settings.editHandleSize;
        var trafo = data.imgTrafo;
        // use given id
        var id = digilib.fn.createId(shape.id, css+'svg-');
        // set properties
        var props = shape.properties || {};
        var stroke = props['stroke'] || settings.defaultStroke;
        var strokeWidth = props['stroke-width'] || settings.defaultStrokeWidth;
        var fill = props['fill'] || settings.defaultFill;
        var coords = shape.geometry.coordinates;
        var gt = shape.geometry.type;
        if (gt === 'Line') {
            /*
             * Line
             */
            var p1 = trafo.transform(geom.position(coords[0]));
            var p2 = trafo.transform(geom.position(coords[1]));
            var $elem = $(createSvg('line', {
                'id': id,
                'x1': p1.x, 'y1': p1.y,
                'x2': p2.x, 'y2': p2.y,
                'stroke': stroke, 'stroke-width': strokeWidth}));
            shape.$elem = $elem;
            $svg.append($elem);
            if (props.editable) {
                var e1 = createSvg('rect', {
                    'x': p1.x-hs/2, 'y': p1.y-hs/2, 'width': hs, 'height': hs,
                    'stroke': 'darkgrey', 'stroke-width': 1, 'fill': 'none',
                    'class': css+'svg-handle', 'style': 'pointer-events:all'});
                var e2 = createSvg('rect', {
                    'x': p2.x-hs/2, 'y': p2.y-hs/2, 'width': hs, 'height': hs,
                    'stroke': 'darkgrey', 'stroke-width': 1, 'fill': 'none',
                    'class': css+'svg-handle', 'style': 'pointer-events:all'});
                var $editElems = $([e1, e2]);
                shape.$editElems = $editElems;
                $svg.append($editElems);
            }
        } else if (gt === 'Rectangle') {
            /*
             * Rectangle
             */
            var p1 = trafo.transform(geom.position(coords[0]));
            var p2 = trafo.transform(geom.position(coords[1]));
            var rect = geom.rectangle(p1, p2);
            var $elem = $(createSvg('rect', {
                'id': id,
                'x': rect.x, 'y': rect.y,
                'width': rect.width, 'height': rect.height,
                'stroke': stroke, 'stroke-width': strokeWidth,
                'fill': fill}));
            shape.$elem = $elem;
            $svg.append($elem);
            if (props.editable) {
                var e1 = createSvg('rect', {
                    'x': p1.x-hs/2, 'y': p1.y-hs/2, 'width': hs, 'height': hs,
                    'stroke': 'darkgrey', 'stroke-width': 1, 'fill': 'none',
                    'class': css+'svg-handle', 'style': 'pointer-events:all'});
                var e2 = createSvg('rect', {
                    'x': p2.x-hs/2, 'y': p2.y-hs/2, 'width': hs, 'height': hs,
                    'stroke': 'darkgrey', 'stroke-width': 1, 'fill': 'none',
                    'class': css+'svg-handle', 'style': 'pointer-events:all'});
                var $editElems = $([e1, e2]);
                shape.$editElems = $editElems;
                $svg.append($editElems);
            }
        }
    };

    var handleUpdate = function (evt) {
        console.debug("vector: handleUpdate");
        var data = this;
        if (data.shapes == null || data.imgTrafo == null || !data.settings.isVectorActive)
            return;
        if (data.zoomArea != data.vectorOldZA) {
            renderShapes(data);
            data.vectorOldZA = data.zoomArea;
        }
        data.$svg.show();
    };

    /** 
     * define a shape by click and drag.
     */
    var defineShape = function(data, shape, onComplete) {
    	var shapeType = shape.geometry.type;
    	var shapeId = shape.id;
    	shapeId = digilib.fn.createId(shapeId, data.settings.cssPrefix+'shape-');
    	shape.id = shapeId;
        var $elem = data.$elem;
        var $scaler = data.$scaler;
        var picRect = geom.rectangle($scaler);
        var $body = $('body');
        var bodyRect = geom.rectangle($body);
        var pt1, pt2;
        // overlay div prevents other elements from reacting to mouse events 
        var $overlayDiv = $('<div class="'+data.settings.cssPrefix+'shapeOverlay" style="position:absolute; z-index:100;"/>');
        $elem.append($overlayDiv);
        bodyRect.adjustDiv($overlayDiv);
        // shape element reference
        var $shape = null;
        var shapeStart = function (evt) {
            pt1 = geom.position(evt);
            // setup and show shape
            p1 = data.imgTrafo.invtransform(pt1);
            if (shapeType === 'Line' || shapeType === 'Rectangle') {
            	shape.geometry.coordinates = [[p1.x, p1.y], [p1.x, p1.y]];
            }
            renderShape(data, shape);
            $shape = shape.$elem;
            // register events
            $overlayDiv.on("mousemove.dlShape", shapeMove);
            $overlayDiv.on("mouseup.dlShape", shapeEnd);
            return false;
        };

        // mouse move handler
        var shapeMove = function (evt) {
            pt2 = geom.position(evt);
            pt2.clipTo(picRect);
            // update shape
            if (shapeType === 'Line') {
            	$shape.attr({'x2': pt2.x, 'y2': pt2.y});
            } else if (shapeType === 'Rectangle') {
                var rect = geom.rectangle(pt1, pt2);
            	$shape.attr({'x': rect.x, 'y': rect.y,
            		'width': rect.width, 'height': rect.height});            	
            }
            return false;
        };

        // mouseup handler: end moving
        var shapeEnd = function (evt) {
            pt2 = geom.position(evt);
            // assume a click and continue if the area is too small
            if (pt2.distance(pt1) < 5) {
            	if (onComplete != null) {
            		onComplete(data, null);
            	}
                return false;
            };
            // unregister events
            $overlayDiv.off("mousemove.dlShape", shapeMove);
            $overlayDiv.off("mouseup.dlShape", shapeEnd);
            // clip and transform
            pt2.clipTo(picRect);
            // update shape
            if (shapeType === 'Line') {
                var p2 = data.imgTrafo.invtransform(pt2);
                shape.geometry.coordinates[1] = [p2.x, p2.y];
            } else if (shapeType === 'Rectangle') {
                var p2 = data.imgTrafo.invtransform(pt2);
                shape.geometry.coordinates[1] = [p2.x, p2.y];
            }
            console.debug("new shape:", shape);
            data.shapes.push(shape);
            $overlayDiv.remove();
        	if (onComplete != null) {
        		onComplete(data, shape);
        	}            
            return false;
        };

        // start by clicking
        $overlayDiv.one('mousedown.dlShape', shapeStart);
    };

    /**
     * create a SVG element
     */
    var createSvg = function (name, attrs) {
        var elem = document.createElementNS(svgNS, name);
        if (attrs != null) {
            for (var att in attrs) {
                elem.setAttributeNS(null, att, attrs[att]);
            };
        }
        return elem;
    };
    
    // plugin object, containing name, install and init routines 
    // all shared objects are filled by digilib on registration
    var plugin = {
            name : 'vector',
            install : install,
            init : init,
            buttons : {},
            actions : {},
            fn : {},
            plugins : {}
    };

    if ($.fn.digilib == null) {
        $.error("jquery.digilib.vector.js must be loaded after jquery.digilib!");
    } else {
        $.fn.digilib('plugin', plugin);
    }
})(jQuery);