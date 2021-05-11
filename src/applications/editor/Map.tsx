import React, { FC, useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import ReactMapGL, { AttributionControl, MapEvent, ScaleControl } from 'react-map-gl';

import { updateViewport } from 'reducers/map';

import colors from 'common/Map/Consts/colors';
import 'common/Map/Map.scss';

/* Main data & layers */
import Background from 'common/Map/Layers/Background';
import OSM from 'common/Map/Layers/OSM';
import Hillshade from 'common/Map/Layers/Hillshade';
import Platform from 'common/Map/Layers/Platform';
import GeoJSONs from 'common/Map/Layers/GeoJSONs';
import EditorZone from 'common/Map/Layers/EditorZone';
import osmBlankStyle from 'common/Map/Layers/osmBlankStyle';

import { Item, PositionnedItem } from '../../types';
import { EditorState } from '../../reducers/editor';

const INTERACTIVE_LAYER_IDS = ['editor/geo-main-layer'];

interface DeepStruct<T> {
  [key: string]: string | T;
}
type Colors = DeepStruct<Colors>;
const COLORS = colors as Record<string, Colors>;

interface MapProps {
  selection: Item[] | null;
  getCursor: (state: { isLoaded: boolean; isDragging: boolean; isHovering: boolean }) => string;
  onClickFeature: (feature: Item) => void | null;
}

const Map: FC<MapProps> = ({ getCursor, selection, onClickFeature }) => {
  const { mapStyle, viewport } = useSelector((state: { map: any }) => state.map);
  const [hovered, setHovered] = useState<PositionnedItem | null>(null);
  const editorState = useSelector((state: { editor: EditorState }) => state.editor);
  const { urlLat, urlLon, urlZoom, urlBearing, urlPitch } = useParams();
  const dispatch = useDispatch();
  const updateViewportChange = useCallback((value) => dispatch(updateViewport(value, '/editor')), [
    dispatch,
  ]);

  /* Interactions */
  const onFeatureHover = useCallback(
    (event: MapEvent) => {
      const feature = (event.features || [])[0];

      if (feature) {
        setHovered({
          id: feature.properties.OP_id as string,
          layer: feature.layer.id as string,
          lng: event.lngLat[0],
          lat: event.lngLat[1],
        });
      } else {
        setHovered(null);
      }
    },
    [editorState]
  );

  useEffect(() => {
    if (urlLat) {
      updateViewportChange({
        ...viewport,
        latitude: parseFloat(urlLat),
        longitude: parseFloat(urlLon),
        zoom: parseFloat(urlZoom),
        bearing: parseFloat(urlBearing),
        pitch: parseFloat(urlPitch),
      });
    }
  }, []);

  return (
    <>
      <ReactMapGL
        {...viewport}
        width="100%"
        height="100%"
        getCursor={getCursor}
        mapStyle={osmBlankStyle}
        onViewportChange={updateViewportChange}
        attributionControl={false} // Defined below
        clickRadius={4}
        interactiveLayerIds={INTERACTIVE_LAYER_IDS}
        onClick={onClickFeature}
        onHover={onFeatureHover}
        touchRotate
        asyncRender
      >
        <AttributionControl
          className="attribution-control"
          customAttribution="©SNCF/DGEX Solutions"
        />
        <ScaleControl
          maxWidth={100}
          unit="metric"
          style={{
            left: 20,
            bottom: 20,
          }}
        />

        {/* OSM layers */}
        <Background colors={COLORS[mapStyle]} />
        <OSM mapStyle={mapStyle} />
        <Hillshade mapStyle={mapStyle} />
        <Platform colors={COLORS[mapStyle]} />

        {/* Editor layers */}
        <EditorZone />
        <GeoJSONs
          colors={COLORS[mapStyle]}
          idHover={hovered && hovered.layer === 'editor/geo-main-layer' ? hovered.id : undefined}
        />
      </ReactMapGL>
    </>
  );
};

export default Map;
