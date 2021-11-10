import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import nextId from 'react-id-generator';
import { DragDropContext, Droppable, Draggable } from 'react-beautiful-dnd';
import { useSelector } from 'react-redux';
import {
  updateViaStopTime,
  permuteVias,
  deleteVias,
} from 'applications/osrd/components/Itinerary/helpers';
import InputSNCF from 'common/BootstrapSNCF/InputSNCF';
import { useDebounce } from 'utils/helpers';

const InputStopTime = (props) => {
  const { index } = props;
  const osrdconf = useSelector((state) => state.osrdconf);
  const [stopTime, setStopTime] = useState(
    osrdconf.vias[index].stoptime ? osrdconf.vias[index].stoptime : 0,
  );
  const [firstStart, setFirstStart] = useState(true);
  const debouncedStopTime = useDebounce(stopTime, 1000);

  useEffect(() => {
    if (!firstStart) {
      updateViaStopTime(index, debouncedStopTime);
      console.log('UPDATE', debouncedStopTime);
    } else {
      setFirstStart(false);
    }
  }, [debouncedStopTime]);

  return (
    <InputSNCF
      type="number"
      id={`osrd-config-stoptime-via-${index}`}
      onChange={
        (e) => setStopTime(e.target.value)
      }
      value={stopTime}
      unit="s"
      sm
      noMargin
    />
  );
};

export default function DisplayVias(props) {
  const osrdconf = useSelector((state) => state.osrdconf);
  const [indexSelected, setIndexSelected] = useState(undefined);
  const { zoomToFeaturePoint } = props;

  return (
    <DragDropContext onDragEnd={(e) => permuteVias(e.source.index, e.destination.index)}>
      <Droppable droppableId="droppableVias">
        {(provided) => (
          <div
            {...provided.droppableProps}
            ref={provided.innerRef}
          >
            {osrdconf.vias.map((place, index) => (
              <Draggable key={`drag-key-${index}`} draggableId={`drag-vias-${index}`} index={index}>
                {(providedDraggable) => (
                  <div
                    ref={providedDraggable.innerRef}
                    {...providedDraggable.draggableProps}
                    {...providedDraggable.dragHandleProps}
                    className="d-flex align-items-center w-100 osrd-config-place osrd-config-place-via"
                  >
                    <i className="text-info icons-itinerary-bullet mr-2" />
                    <div className="pl-1 hover w-100 d-flex align-items-center">
                      <div
                        className="flex-grow-1"
                        onClick={() => zoomToFeaturePoint(
                          place.clickLngLat,
                          place.id,
                          place.source,
                        )}
                        role="button"
                        tabIndex={0}
                      >
                        <small className="font-weight-bold text-muted mr-1">{index + 1}</small>
                        <small className="mr-1 text-nowrap">{`${place.id} ${place.name}`}</small>
                        {/* <div className="small text-nowrap ml-3">
                          {`${place.pkSncfDe} • ${place.pkSncfFi}`}</div> */}
                      </div>
                      <div className="osrd-config-stoptime" role="button" tabIndex="-1" onClick={() => setIndexSelected(index)}>
                        {index === indexSelected
                          ? (
                            <InputStopTime
                              index={index}
                            />
                          ) : (
                            <>
                              {osrdconf.vias[index].stoptime ? osrdconf.vias[index].stoptime : 0}
                              <i className="ml-2 icons-pencil" />
                            </>
                          )}
                      </div>
                      <button
                        className="btn btn-sm btn-only-icon btn-white ml-auto"
                        type="button"
                        onClick={() => deleteVias(index)}
                      >
                        <i className="icons-circle-delete" />
                        <span className="sr-only" aria-hidden="true">Delete</span>
                      </button>
                    </div>
                  </div>
                )}
              </Draggable>
            ))}
            {provided.placeholder}
          </div>
        )}
      </Droppable>
    </DragDropContext>
  );
}

DisplayVias.propTypes = {
  zoomToFeaturePoint: PropTypes.func.isRequired,
};

InputStopTime.propTypes = {
  index: PropTypes.number.isRequired,
  value: PropTypes.number,
};
InputStopTime.defaultProps = {
  value: 0,
};